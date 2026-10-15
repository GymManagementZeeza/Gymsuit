"""
Daily Retraining Service for GymSuit Food Image Classifier.

This script runs every night (scheduled via systemd timer or cron).
It scans all dish subdirectories in FoodImageTraining/, trains on all
available images, updates model/food_model.pt and model/labels.json,
and triggers a zero-downtime hot-reload of the inference server.

Usage:
    python daily_retrain.py [--data .] [--epochs 25] [--lr 1e-3]
"""

import argparse
import datetime
import json
import logging
import os
import shutil
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import torch
import torch.nn as nn
from PIL import Image
from torch.utils.data import DataLoader, Dataset
from torchvision import models, transforms

# Configure logging
LOG_FORMAT = "%(asctime)s [%(levelname)s] %(message)s"
logging.basicConfig(level=logging.INFO, format=LOG_FORMAT)
logger = logging.getLogger("daily_retrain")

IMG_EXTS = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}
IGNORED_DIRS = {"model", "__pycache__", "build", "dist", "venv", ".venv"}

NORMALIZE = transforms.Normalize(
    mean=[0.485, 0.456, 0.406],
    std=[0.229, 0.224, 0.225]
)

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


class SafeImageFolderDataset(Dataset):
    """Dataset that safely loads images, skipping or falling back on corrupt files."""
    def __init__(self, samples, transform):
        self.samples = samples  # list of (Path, class_idx)
        self.transform = transform

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, i):
        path, label = self.samples[i]
        try:
            img = Image.open(path).convert("RGB")
        except Exception as e:
            logger.warning(f"Error reading image {path}: {e}, substituting with blank canvas")
            img = Image.new("RGB", (224, 224), color=(128, 128, 128))
        return self.transform(img), label


def scan_dataset(data_dir: Path):
    """
    Scan all non-ignored directories.
    Returns (classes, valid_samples, class_counts).
    """
    candidates = sorted(
        p for p in data_dir.iterdir()
        if p.is_dir() and not p.name.startswith((".", "_")) and p.name not in IGNORED_DIRS
    )

    classes = []
    samples = []
    class_counts = {}

    for p in candidates:
        files = sorted(
            f for f in p.iterdir()
            if f.suffix.lower() in IMG_EXTS and f.is_file()
        )
        if not files:
            logger.warning(f"Folder '{p.name}' is empty or has no supported image files. Skipping.")
            continue

        idx = len(classes)
        classes.append(p.name)
        class_counts[p.name] = len(files)
        samples.extend((f, idx) for f in files)

    return classes, samples, class_counts


def split_train_val(samples, classes, seed=42):
    """Hold out 1 image per class for validation when class has >= 2 images."""
    import random
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


def reload_inference_server(reload_url="http://localhost:8001/reload", health_url="http://localhost:8001/health"):
    """Trigger zero-downtime reload on inference server."""
    logger.info(f"Triggering hot-reload at {reload_url}...")
    try:
        req = urllib.request.Request(reload_url, data=b"{}", headers={"Content-Type": "application/json"}, method="POST")
        with urllib.request.urlopen(req, timeout=5) as response:
            res_body = response.read().decode("utf-8")
            logger.info(f"Hot-reload response: {res_body}")
    except Exception as e:
        logger.warning(f"Could not reach {reload_url}: {e}. Retrying health check directly...")

    # Verify health
    try:
        with urllib.request.urlopen(health_url, timeout=5) as resp:
            health_body = resp.read().decode("utf-8")
            logger.info(f"Inference server health: {health_body}")
            return True
    except Exception as e:
        logger.error(f"Inference server health check failed: {e}")
        return False


def retrain(data_path=".", output_dir="model", epochs=25, lr=1e-3, reload_url="http://localhost:8001/reload"):
    start_time = time.time()
    data_dir = Path(data_path).resolve()
    out_dir = Path(output_dir).resolve()
    out_dir.mkdir(parents=True, exist_ok=True)

    logger.info("=" * 60)
    logger.info(f"Starting daily retraining job at {datetime.datetime.now().isoformat()}")
    logger.info(f"Dataset root: {data_dir}")

    classes, samples, class_counts = scan_dataset(data_dir)

    if len(classes) < 2:
        logger.error(f"Need at least 2 dish folders with images to train. Found {len(classes)}: {classes}")
        return False

    logger.info(f"Discovered {len(classes)} dish classes ({len(samples)} total images):")
    for cls in classes:
        logger.info(f"  • {cls}: {class_counts[cls]} images")

    train_samples, val_samples = split_train_val(samples, classes)
    logger.info(f"Split: {len(train_samples)} training | {len(val_samples)} validation")

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    logger.info(f"Compute device: {device}")

    # Dynamic batch size based on dataset size
    batch_size = min(32, max(2, len(train_samples) // 2)) if len(train_samples) > 2 else len(train_samples)
    logger.info(f"Using batch size: {batch_size}, epochs: {epochs}, learning rate: {lr}")

    train_ds = SafeImageFolderDataset(train_samples, TRAIN_TF)
    val_ds = SafeImageFolderDataset(val_samples, EVAL_TF)
    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True)
    val_loader = DataLoader(val_ds, batch_size=batch_size) if val_samples else None

    model = build_model(len(classes)).to(device)
    criterion = nn.CrossEntropyLoss()
    optimizer = torch.optim.Adam(
        (p for p in model.parameters() if p.requires_grad), lr=lr
    )

    best_loss = float("inf")
    last_train_acc = 0.0
    last_val_acc = 0.0

    for epoch in range(1, epochs + 1):
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

        epoch_loss = total_loss / max(total, 1)
        last_train_acc = correct / max(total, 1)
        best_loss = min(best_loss, epoch_loss)

        msg = f"Epoch {epoch:02d}/{epochs:02d} | loss={epoch_loss:.4f} | train_acc={last_train_acc:.1%}"

        if val_loader:
            model.eval()
            v_correct, v_total = 0, 0
            with torch.no_grad():
                for images, labels in val_loader:
                    outputs = model(images.to(device))
                    v_correct += (outputs.argmax(1) == labels.to(device)).sum().item()
                    v_total += images.size(0)
            last_val_acc = v_correct / max(v_total, 1)
            msg += f" | val_acc={last_val_acc:.1%}"

        if epoch % 5 == 0 or epoch == epochs:
            logger.info(msg)

    # Backup existing weights if present
    pt_file = out_dir / "food_model.pt"
    json_file = out_dir / "labels.json"
    if pt_file.exists():
        shutil.copy2(pt_file, out_dir / "food_model.pt.bak")
    if json_file.exists():
        shutil.copy2(json_file, out_dir / "labels.json.bak")

    # Atomic write to temporary files then rename
    tmp_pt = out_dir / "food_model.pt.tmp"
    tmp_json = out_dir / "labels.json.tmp"

    torch.save({"state_dict": model.state_dict(), "classes": classes}, tmp_pt)
    tmp_json.write_text(json.dumps(classes, indent=2))

    tmp_pt.replace(pt_file)
    tmp_json.replace(json_file)

    duration = round(time.time() - start_time, 2)
    logger.info(f"Retraining completed in {duration}s. Saved {pt_file} & {json_file}")

    # Record history log
    history_file = out_dir / "training_history.jsonl"
    record = {
        "timestamp": datetime.datetime.now().isoformat(),
        "classes": classes,
        "class_count": len(classes),
        "total_images": len(samples),
        "epochs": epochs,
        "final_loss": round(best_loss, 4),
        "train_accuracy": round(last_train_acc, 4),
        "val_accuracy": round(last_val_acc, 4),
        "duration_sec": duration,
    }
    with open(history_file, "a") as f:
        f.write(json.dumps(record) + "\n")

    # Reload inference server
    reload_inference_server(reload_url)

    logger.info("Daily retraining job finished successfully!")
    logger.info("=" * 60)
    return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Daily Retraining Service for GymSuit Food Model.")
    parser.add_argument("--data", default=".", help="Dataset root directory")
    parser.add_argument("--output", default="model", help="Directory where model artifacts are saved")
    parser.add_argument("--epochs", type=int, default=25, help="Number of training epochs")
    parser.add_argument("--lr", type=float, default=1e-3, help="Learning rate")
    parser.add_argument("--reload-url", default="http://localhost:8001/reload", help="Hot-reload URL for inference server")
    args = parser.parse_args()

    success = retrain(
        data_path=args.data,
        output_dir=args.output,
        epochs=args.epochs,
        lr=args.lr,
        reload_url=args.reload_url,
    )
    sys.exit(0 if success else 1)
