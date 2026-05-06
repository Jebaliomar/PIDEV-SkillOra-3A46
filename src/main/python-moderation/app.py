from fastapi import FastAPI
from pydantic import BaseModel
from detoxify import Detoxify

app = FastAPI()

# Use the multilingual Detoxify model for English, French, and other supported languages.
model = Detoxify("multilingual")

class ModerationRequest(BaseModel):
    text: str


def to_float(value):
    return float(value)


@app.post("/moderate")
def moderate_text(payload: ModerationRequest):
    scores = model.predict(payload.text)

    # Scores returned by Detoxify include toxicity-related labels.
    return {
        "toxicity": to_float(scores["toxicity"]),
        "severe_toxicity": to_float(scores["severe_toxicity"]),
        "obscene": to_float(scores["obscene"]),
        "threat": to_float(scores["threat"]),
        "insult": to_float(scores["insult"]),
        "identity_attack": to_float(scores["identity_attack"]),
    }
