# # Use an official Python runtime as a parent image
# FROM python:3.10 as builder

# RUN apt-get update && apt-get install -y --no-install-recommends build-essential

# WORKDIR /app

# RUN python -m venv /opt/venv
# ENV PATH="/opt/venv/bin:$PATH"

# COPY requirements.txt .

# RUN pip install --no-cache-dir -r requirements.txt

# FROM python:3.10
 
# RUN apt-get update && \
#     apt-get install -y --no-install-recommends ffmpeg && \
#     rm -rf /var/lib/apt/lists/*

# WORKDIR /app

# COPY --from=builder /opt/venv /opt/venv

# COPY . .

# ENV PATH="/opt/venv/bin:$PATH"
 
# EXPOSE 8080

# CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]

# Use an official NVIDIA CUDA runtime as a parent image
# This image includes CUDA 12.1.1 and cuDNN 8, required for GPU acceleration.
FROM nvidia/cuda:12.8.1-cudnn-devel-ubuntu22.04

# Install Python 3.10, pip, venv, build-essential and ffmpeg
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    python3.10 \
    python3.10-venv \
    python3-pip \
    build-essential \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# Link python3 to python
RUN ln -s /usr/bin/python3 /usr/bin/python

WORKDIR /app

# Create and activate virtual environment
RUN python -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

# Copy and install requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application code
COPY . .

EXPOSE 8080

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]
