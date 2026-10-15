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

## Adding New Images & Dishes

- **To add images to existing dishes:** Drop new `.jpg`, `.png`, or `.webp` photos into the dish folder (e.g. `dosa/`, `beeffry/`).
- **To add a new dish:** Create a new folder (e.g. `biryani/` or `chicken_curry/` using lowercase and underscores) and add images inside it.
- Aim for 50–100 varied photos per dish for production accuracy.

## Automated Daily Retraining (Midnight Service)

The server runs an automated retraining job every night at midnight via systemd timer:

```bash
# Run manually anytime
python daily_retrain.py --epochs 25

# Check status of the midnight timer
systemctl status gymsuit-food-retrain.timer

# View logs of the daily retraining job
journalctl -u gymsuit-food-retrain.service -e
```

When new images or folders are added, the nightly job:
1. Automatically scans all dish folders.
2. Trains MobileNetV2 with data augmentation.
3. Atomically updates `model/food_model.pt` and `model/labels.json`.
4. Hot-reloads the running inference server (`POST /reload`) with zero downtime.
5. Appends metrics to `model/training_history.jsonl`.

## Step 1 — Train (run manually)

```bash
cd FoodImageTraining
pip install -r requirements.txt
python train.py            # or: python daily_retrain.py --epochs 25
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
- `POST /reload` → reloads model from disk with zero downtime
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
