from confluent_kafka import Consumer, KafkaError, Producer
import os
from dotenv import load_dotenv
import logging
from schemas.transcribe import TranscribeReq
from service.minute_service import get_minute
import json
import time

logger = logging.getLogger(__name__)
load_dotenv()

BATCH_SIZE = 10
BATCH_FLUSH_INTERVAL = 2


def run_transcribe_pipeline():
    conf = {
        "bootstrap.servers": os.getenv("KAFKA_BOOTSTRAP_SERVERS"),
        "group.id": os.getenv("KAFKA_GROUP_ID"),
        "enable.auto.commit": False,
        'max.poll.interval.ms': 1800000,
    }

    producer_conf = {"bootstrap.servers": os.getenv("KAFKA_BOOTSTRAP_SERVERS")}

    producer = Producer(producer_conf)
    consumer = Consumer(conf)

    consumer_topic = "transcribe-request"
    producer_topic = "transcribe-response"
    consumer.subscribe([consumer_topic])

    print(f"전사문 컨슈머 시작 : {consumer_topic}")

    batch_counter = 0
    last_flush_time = time.time()

    try:
        while True:
            msg = consumer.poll(timeout=1.0)

            now = time.time()
            flush_due = batch_counter > 0 and (
                now - last_flush_time > BATCH_FLUSH_INTERVAL
            )

            if flush_due:
                try:
                    producer.flush()
                    batch_counter = 0
                    last_flush_time = now
                except Exception as e:
                    logger.error(f"Timeout flush failed: {e}")

            if msg is None or msg.error():
                continue

            try:
                req = TranscribeReq.model_validate_json(msg.value().decode("utf-8"))
                result = get_minute(req)

                producer.produce(
                    topic=producer_topic,
                    value=json.dumps(result, default=str).encode("utf-8"),
                    key=None,
                )
                producer.flush()

                consumer.commit(asynchronous=False)

                logger.info(f"Transcribe request received: {req}, {type(req)}")
                logger.info(f"consumed: {msg.topic}, {msg.value}")

            except Exception as e:
                print(f"메시지 처리 중 오류 발생: {e}")
                # 예외 시 커밋하지 않음 → 재처리 가능

    finally:
        try:
            producer.flush()
        except Exception as e:
            logger.error(f"Final flush failed: {e}")
        consumer.close()
        producer.close()
        print("전사문 컨슈머, 프로듀서 종료")
