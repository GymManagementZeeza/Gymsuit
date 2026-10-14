"""
Food-image inference server for GymSuit.

Serves the model trained by train.py. The Spring Boot backend forwards
uploaded dish photos here and returns the prediction to the app.

Run (from this folder, after training):
    uvicorn inference_server:app --host 0.0.0.0 --port 8001

Endpoints:
    GET  /health   -> {"status": "ok", "classes": [...]}
    POST /predict  -> multipart form, field "image" (one image file)
                      returns {"dish": ..., "confidence": ..., "probabilities": {...}}
"""

import io
import json
import os
from pathlib import Path

import torch
import torch.nn as nn
from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image
from torchvision import models, transforms

MODEL_PATH = Path(__file__).parent / "model" / "food_model.pt"
LABELS_PATH = Path(__file__).parent / "model" / "labels.json"

PREPROCESS = transforms.Compose([
    transforms.Resize(256),
    transforms.CenterCrop(224),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
])

app = FastAPI(title="GymSuit Food Recognition")
_model = None
_classes = None


def load_model():
    global _model, _classes
    if _model is not None:
        return _model, _classes
    if not MODEL_PATH.exists():
        raise RuntimeError(
            f"Model not found at {MODEL_PATH}. Run `python train.py` first."
        )
    _classes = json.loads(LABELS_PATH.read_text())
    checkpoint = torch.load(MODEL_PATH, map_location="cpu", weights_only=False)
    _classes = checkpoint.get("classes", _classes)
    model = models.mobilenet_v2(weights=None)
    in_features = model.classifier[1].in_features
    model.classifier = nn.Sequential(
        nn.Dropout(p=0.2),
        nn.Linear(in_features, len(_classes)),
    )
    model.load_state_dict(checkpoint["state_dict"])
    model.eval()
    _model = model
    return _model, _classes


@app.on_event("startup")
def _startup():
    load_model()


@app.get("/health")
def health():
    _, classes = load_model()
    return {"status": "ok", "classes": classes}


@app.post("/predict")
async def predict(image: UploadFile = File(...)):
    model, classes = load_model()
    if not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="Uploaded file is not an image.")
    try:
        img = Image.open(io.BytesIO(await image.read())).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="Could not read image file.")
    with torch.no_grad():
        probs = torch.softmax(model(PREPROCESS(img).unsqueeze(0)), dim=1)[0]
    idx = int(probs.argmax())
    return {
        "dish": classes[idx],
        "confidence": round(float(probs[idx]), 4),
        "probabilities": {
            classes[i]: round(float(probs[i]), 4) for i in range(len(classes))
        },
    }
