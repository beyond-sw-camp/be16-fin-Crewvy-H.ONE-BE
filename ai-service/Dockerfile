# ---------------------------------------------------------------------
# Stage 1: Builder (빌드 도구 포함)
# CUDA 12.8.0 버전 사용 (정확한 태그명은 Docker Hub 확인 필요)
# 예: nvidia/cuda:12.8.0-cudnn9-devel-ubuntu22.04
FROM nvidia/cuda:13.0.2-cudnn-devel-ubuntu22.04 AS builder

# Python 3.10 및 `venv` 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3.10 \
    python3-pip \
    python3-venv \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 가상환경 생성
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:$PATH"

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ---------------------------------------------------------------------
# Stage 2: Runtime (실행용 경량 이미지)
# Builder와 동일한 OS 및 CUDA 버전 사용
FROM nvidia/cuda:13.0.2-cudnn-runtime-ubuntu22.04

# 런타임에 필요한 패키지 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    ffmpeg \
    python3.10 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Builder에서 생성한 가상환경 복사
COPY --from=builder /opt/venv /opt/venv

COPY . .

# 가상환경 PATH 적용
ENV PATH="/opt/venv/bin:$PATH"

EXPOSE 8080

# uvicorn 실행
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8080"]