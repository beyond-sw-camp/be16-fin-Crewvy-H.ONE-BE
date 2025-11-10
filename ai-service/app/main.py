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

@asynccontextmanager
async def lifespan(app_: FastAPI):
    # Startup
    # host_ip = "localhost"
    # port = int(os.getenv("INSTANCE_PORT", 8000))
    # logger.info(f"유레카 클라이언트 등록 시작 - host: {host_ip}, port: {port}")
    # await eureka_client.init_async(
    #     eureka_server="http://localhost:8761/eureka/",
    #     app_name="ai-service",
    #     instance_port=port,
    #     instance_host=host_ip,
    #     instance_ip=host_ip,
    # )
    # run_transcribe_pipeline()

    kafka_task = asyncio.create_task(run_pipeline_in_background())

    logger.info("Eureka client initialization skipped for testing")
    yield
    # Shutdown
    kafka_task.cancel()
    try:
        await kafka_task
    except asyncio.CancelledError:
        logger.info("Kafka pipeline task cancelled successfully")
    logger.info("Stopping Eureka client")
    # await eureka_client.stop_async()


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
