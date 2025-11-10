from fastapi import *
from pydantic import BaseModel
from service.minute_service import *
from schemas.transcribe import TranscribeReq
import logging
import uvicorn
from contextlib import asynccontextmanager
from py_eureka_client import eureka_client
import socket
import os
import asyncio
from service.kafka_service import run_transcribe_pipeline, run_pipeline_in_background

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s : %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)

from contextlib import asynccontextmanager
from concurrent.futures import ProcessPoolExecutor

# CPU-bound 작업을 위한 ProcessPoolExecutor 생성
process_pool_executor = ProcessPoolExecutor(max_workers=1)


@asynccontextmanager
async def lifespan(app_: FastAPI):
    # Startup
    loop = asyncio.get_running_loop()
    kafka_task = loop.create_task(
        run_pipeline_in_background(loop, process_pool_executor)
    )
    logger.info("AI 서비스 시작. 백그라운드에서 Kafka 컨슈머 실행.")

    yield

    # Shutdown
    logger.info("AI 서비스 종료 시작.")
    # Kafka 태스크 취소 및 종료 대기
    kafka_task.cancel()
    try:
        await kafka_task
    except asyncio.CancelledError:
        logger.info("Kafka 파이프라인 태스크가 성공적으로 취소되었습니다.")

    # Executor 종료
    process_pool_executor.shutdown(wait=True)
    logger.info("ProcessPoolExecutor가 정상적으로 종료되었습니다.")
    logger.info("AI 서비스 종료 완료.")


app = FastAPI(lifespan=lifespan)


@app.get("/actuator/health", status_code=status.HTTP_200_OK)
def health():
    logger.info("헬스 체크 요청 수신")
    return {"status": "UP"}


@app.post("/transcribe", status_code=status.HTTP_202_ACCEPTED)
def transcribe_video(transcribe_req: TranscribeReq):
    logger.info("전사문 생성 요청 수신")
    return get_minute(transcribe_req)


def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(("", 0))
        return s.getsockname()[1]


if __name__ == "__main__":
    port = find_free_port()
    os.environ["INSTANCE_PORT"] = str(port)
    print(f"Starting server on port {port}")
    uvicorn.run("main:app", host="0.0.0.0", port=port)
