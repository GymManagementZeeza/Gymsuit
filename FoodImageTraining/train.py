"""
Train the GymSuit dish classifier.

Usage (run manually, from this folder):
    pip install -r requirements.txt
    python train.py [--epochs 15] [--data .] [--output model]

Dataset layout (add more images any time, then re-run):
    FoodImageTraining/
        kerala_porotta/*.jpg
        beeffry/*.jpg
        dosa/*.jpg
        idli/*.jpg

Method: transfer learning. A MobileNetV2 pre-trained on ImageNet keeps its
frozen feature extractor; only a small classifier head is trained on your
images. With heavy augmentation this converges on CPU in a couple of minutes
for a small dataset.

Output:
    model/food_model.pt  - weights + class names (used by inference_server.py)
    model/labels.json    - ordered class names (index -> dish)

Honest note: 2 images per class only proves the pipeline. Expect unreliable
predictions until each class has ~50-100 varied photos (different plates,
lighting, angles). Add images and re-run this script.
"""

import argparse
import json
import os
import random
import sys
from pathlib import Path

import torch
import torch.nn as nn
from PIL import Image
from torch.utils.data import DataLoader, Dataset
from torchvision import models, transforms

IMG_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}

# ImageNet mean/std — matches the MobileNetV2 pre-training.
NORMALIZE = transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])

TRAIN_TF = transforms.Compose([
    transforms.RandomResizedCrop(224, scale=(0.7, 1.0)),
    transforms.RandomHorizontalFlip(),
    transforms.RandomRotation(15),
    transforms.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2),
    transforms.ToTensor(),
    NORMALIZE,
])

EVAL_TF = transforms.Compose([
    transforms.Resize(256),
    transforms.CenterCrop(224),
    transforms.ToTensor(),
    NORMALIZE,
])


class ImageFolderDataset(Dataset):
    def __init__(self, samples, transform):
        self.samples = samples  # list of (path, class_idx)
        self.transform = transform

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, i):
        path, label = self.samples[i]
        img = Image.open(path).convert("RGB")
        return self.transform(img), label


IGNORED_DIRS = {"model", "__pycache__", "build", "dist", "venv", ".venv"}


def collect_samples(data_dir: Path):
    """Return (classes, samples) where classes are sorted subfolder names containing images."""
    candidates = sorted(
        p for p in data_dir.iterdir()
        if p.is_dir() and not p.name.startswith((".", "_")) and p.name not in IGNORED_DIRS
    )
    classes = []
    samples = []
    for p in candidates:
        files = sorted(
            f for f in p.iterdir()
            if f.suffix.lower() in IMG_EXTS and f.is_file()
        )
        if not files:
            print(f"WARNING: class folder '{p.name}' has no valid images, skipping.")
            continue
        idx = len(classes)
        classes.append(p.name)
        samples.extend((f, idx) for f in files)

    if len(classes) < 2:
        sys.exit(f"Need at least 2 class folders with images in {data_dir}, found: {classes}")
    if not samples:
        sys.exit(f"No images found under {data_dir}")
    return classes, samples


def split_train_val(samples, classes, seed=42):
    """Hold out 1 image per class for validation when the class has >= 2."""
    rng = random.Random(seed)
    by_class = {c: [] for c in range(len(classes))}
    for s in samples:
        by_class[s[1]].append(s)
    train, val = [], []
    for idx, items in by_class.items():
        rng.shuffle(items)
        if len(items) >= 2:
            val.append(items[0])
            train.extend(items[1:])
        else:
            train.extend(items)
    return train, val


def build_model(num_classes: int):
    weights = models.MobileNet_V2_Weights.IMAGENET1K_V1
    model = models.mobilenet_v2(weights=weights)
    for param in model.features.parameters():
        param.requires_grad = False
    in_features = model.classifier[1].in_features
    model.classifier = nn.Sequential(
        nn.Dropout(p=0.2),
        nn.Linear(in_features, num_classes),
    )
    return model


def main():
    parser = argparse.ArgumentParser(description="Train the dish classifier.")
    parser.add_argument("--data", default=".", help="Dataset root (class subfolders).")
    parser.add_argument("--epochs", type=int, default=15, help="Training epochs.")
    parser.add_argument("--output", default="model", help="Output dir for model + labels.")
    parser.add_argument("--lr", type=float, default=1e-3, help="Learning rate.")
    args = parser.parse_args()

    data_dir = Path(args.data)
    classes, samples = collect_samples(data_dir)
    train_samples, val_samples = split_train_val(samples, classes)
    print(f"Classes ({len(classes)}): {classes}")
    print(f"Train: {len(train_samples)} images | Val: {len(val_samples)} images")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Device: {device}")

    train_ds = ImageFolderDataset(train_samples, TRAIN_TF)
    val_ds = ImageFolderDataset(val_samples, EVAL_TF)
    train_loader = DataLoader(train_ds, batch_size=8, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=8) if val_samples else None

    model = build_model(len(classes)).to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(
        (p for p in model.parameters() if p.requires_grad), lr=args.lr
    )

    for epoch in range(1, args.epochs + 1):
        model.train()
        total_loss, correct, total = 0.0, 0, 0
        for images, labels in train_loader:
            images, labels = images.to(device), labels.to(device)
            optimizer.zero_grad()
            outputs = model(images)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            total_loss += loss.item() * images.size(0)
            correct += (outputs.argmax(1) == labels).sum().item()
            total += images.size(0)
        msg = f"Epoch {epoch}/{args.epochs}  loss={total_loss / max(total, 1):.4f}  train_acc={correct / max(total, 1):.2%}"
        if val_loader:
            model.eval()
            v_correct, v_total = 0, 0
            with torch.no_grad():
                for images, labels in val_loader:
                    outputs = model(images.to(device))
                    v_correct += (outputs.argmax(1) == labels.to(device)).sum().item()
                    v_total += images.size(0)
            msg += f"  val_acc={v_correct / max(v_total, 1):.2%}"
        print(msg)

    out_dir = Path(args.output)
    out_dir.mkdir(parents=True, exist_ok=True)
    torch.save({"state_dict": model.state_dict(), "classes": classes},
               out_dir / "food_model.pt")
    (out_dir / "labels.json").write_text(json.dumps(classes, indent=2))
    print(f"\nSaved {out_dir / 'food_model.pt'} and {out_dir / 'labels.json'}")
    print("Start the API with:  uvicorn inference_server:app --port 8001")


if __name__ == "__main__":
    main()
