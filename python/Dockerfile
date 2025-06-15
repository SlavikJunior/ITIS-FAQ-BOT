FROM python:3.9-slim
WORKDIR /app

COPY main.py data.csv requirements.txt ./

RUN pip install --no-cache-dir -r requirements.txt

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]