# Use an official Python runtime as a parent image
FROM python:3.10 as builder

RUN apt-get update && apt-get install -y --no-install-recommends build-essential
 
WORKDIR /app

RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

FROM python:3.10

RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /opt/venv /opt/venv

COPY . .

ENV PATH="/opt/venv/bin:$PATH"

EXPOSE 8080
#dfadasfadsf
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]