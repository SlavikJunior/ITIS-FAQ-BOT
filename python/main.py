import pandas as pd
from sentence_transformers import SentenceTransformer, util
import gdown
from fastapi import FastAPI
from pydantic import BaseModel

file_id = "1FA4ktrVxI09s7z0Fcw_T9rfNFcxEEK1q"
file_url = f"https://drive.google.com/uc?id={file_id}"
output = "data.csv"
gdown.download(file_url, output, quiet=True)

df = pd.read_csv(output)
questions = df["Вопрос"].tolist()
answers = df["Ответ"].tolist()

model = SentenceTransformer('all-MiniLM-L6-v2')
question_embeddings = model.encode(questions)

def get_answer(user_question, threshold=0.7):
    user_embedding = model.encode(user_question)
    similarities = util.cos_sim(user_embedding, question_embeddings)
    best_match_idx = similarities.argmax().item()
    best_score = similarities[0][best_match_idx].item()

    if best_score < threshold:
        return "Извините, не нашёл ответа. Обратитесь к сотрудникам ИТИС с вопросом"
    return answers[best_match_idx]

app = FastAPI()

class QuestionRequest(BaseModel):
    question: str

@app.post("/ask")
def ask(question_request: QuestionRequest):
    answer = get_answer(question_request.question)
    return {"answer": answer}

# uvicorn main:app --reload
# pip install fastapi uvicorn pandas gdown sentence-transformers