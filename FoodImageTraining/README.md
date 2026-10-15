# Food Image Training — Dish Recognition

Train a small image classifier that identifies a dish (kerala_porotta, beeffry,
dosa, idli) from a photo, and serve it behind the GymSuit backend API.

## Dataset

Each subfolder is one class. The folder for Kerala porotta is named
`kerala_porotta` (underscore instead of a space) to avoid path headaches in
scripts and commands. Add your own photos any time, then re-train:

```
FoodImageTraining/
    kerala_porotta/   kerala_porotta_1.jpg, kerala_porotta_2.jpg, ...
    beeffry/          beeffry_1.jpg, beeffry_2.jpg, ...
    dosa/             dosa_1.png, dosa_2.jpg, ...
    idli/             idli_1.jpg, idli_2.png, ...
```

The 8 starter images are samples from the web (recipe sites) to prove the
pipeline — each one was opened and checked to confirm it shows the right dish.
For production, replace them with your own photos — aim for 50–100 varied
shots per dish (different plates, lighting, angles). With only 2 images per
class, predictions will be unreliable.

## Step 1 — Train (run manually)

```bash
cd FoodImageTraining
pip install -r requirements.txt
python train.py            # or: python train.py --epochs 25
```

Output: `model/food_model.pt` (weights + class names) and `model/labels.json`.

Method: MobileNetV2 pre-trained on ImageNet, frozen backbone, only the
classifier head is trained, with augmentation (crop / flip / rotation / color).
Runs on CPU; a small dataset trains in a couple of minutes. Add more images
anytime and re-run this script.

## Step 2 — Serve the model

```bash
uvicorn inference_server:app --host 0.0.0.0 --port 8001
```

- `GET /health` → `{"status": "ok", "classes": [...]}`
- `POST /predict` → multipart form, field `image`, returns
  `{"dish": "dosa", "confidence": 0.93, "probabilities": {...}}`

## Step 3 — Use the backend API

The Spring Boot backend exposes:

```
POST /api/mobile/food/recognize   (authenticated, multipart field "image")
```

`FoodRecognitionController` forwards the image to the Python inference server
(`app.food-recognition.url`, default `http://localhost:8001/predict`) and
returns `{"dish", "confidence", "probabilities"}` to the app.

- If the inference server is not running, the API answers **503** with a
  message telling you to start it: `uvicorn inference_server:app --port 8001`
  (from `FoodImageTraining/`).
- On the deployment server, run the inference server alongside the backend
  (same host) and point the property at it via the `FOOD_RECOGNITION_URL`
  environment variable.

## Files

| File | Purpose |
| --- | --- |
| `train.py` | Manual training script |
| `inference_server.py` | FastAPI inference server (`/health`, `/predict`) |
| `requirements.txt` | Python dependencies (torch, torchvision, FastAPI, ...) |
| `model/` | Created by training: `food_model.pt`, `labels.json` |
