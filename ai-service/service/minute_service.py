import whisperx

from whisperx.diarize import DiarizationPipeline
from schemas.transcribe import TranscribeReq
import os
from dotenv import load_dotenv
import boto3
import logging
import shutil
from datetime import datetime
import gc
import torch
import json
from service.ax4_service import generate_summary

logger = logging.getLogger(__name__)
load_dotenv()

device = os.getenv("WHISPERX_DEVICE")
compute_type = os.getenv("WHISPERX_COMPUTE_TYPE")
use_auth_token = os.getenv("HUGGINGFACE_TOKEN")


# 영상 -> 회의록 생성
def transcribe(audio_file, batch_size=16):
    model_dir = "./models/whisperx/"
    model = whisperx.load_model(
        "large-v3",
        device,
        compute_type=compute_type,
        download_root=model_dir,
    )

    logger.info("WhisperX 전사 시작")
    torch.backends.cuda.matmul.allow_tf32 = True
    torch.backends.cudnn.allow_tf32 = True
    audio = whisperx.load_audio(audio_file)
    result = model.transcribe(
        audio,
        batch_size=batch_size,
    )
    logger.info("WhisperX 전사 완료")

    gc.collect()
    torch.cuda.empty_cache()
    del model

    logger.info("WhisperX 정렬 시작")

    model_a, metadata = whisperx.load_align_model(
        language_code=result["language"], device=device
    )
    result = whisperx.align(
        result["segments"],
        model_a,
        metadata,
        audio,
        device,
        return_char_alignments=False,
    )

    logger.info("WhisperX 정렬 완료")

    gc.collect()
    torch.cuda.empty_cache()
    del model_a

    logger.info("WhisperX 화자 분리 시작")

    diarize_model = DiarizationPipeline(use_auth_token=use_auth_token, device=device)

    diarize_segments = diarize_model(audio)

    logger.info("WhisperX 화자 분리 완료")

    logger.info("WhisperX 화자 할당 시작")

    result = whisperx.assign_word_speakers(diarize_segments, result)

    logger.info("WhisperX 화자 할당 완료")

    # 메모리 정리
    del diarize_model, audio
    gc.collect()
    torch.cuda.empty_cache()

    return result["segments"]


def clean_hallucination_segments(segments, vad_threshold=0.1):
    """
    무음(또는 음성 활동이 거의 없는) segment에서 환청 텍스트를 제거.
    - vad_threshold: 음성 길이 최소값(초), 너무 짧으면 환청으로 간주
    """
    cleaned = []
    for seg in segments:
        if (seg.get("end", 0) - seg.get("start", 0) < vad_threshold): 
            continue
        cleaned.append(seg)
    return cleaned

# 회의록 생성 요청 처리
def get_minute(recording: TranscribeReq):
    start_time = datetime.now()
    bucket_name = os.getenv("AWS_S3_BUCKET_NAME")
    access_key = os.getenv("AWS_S3_ACCESS_KEY")
    secret_key = os.getenv("AWS_S3_SECRET_KEY")

    s3 = boto3.client(
        "s3",
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
    )

    if not os.path.exists(f"./tmp/recordings/{recording.videoConferenceId}"):
        os.makedirs(f"./tmp/recordings/{recording.videoConferenceId}")

    s3.download_file(bucket_name, recording.filename, f"./tmp/{recording.filename}")
    logger.info(f"S3에서 파일 다운로드 완료: {recording.filename}")

    result = transcribe(
        audio_file=f"./tmp/{recording.filename}",
        batch_size=16,
    )

    result = clean_hallucination_segments(result)

    text_list = []
    texts = ""
    
    for segment in result:
        item = {
            "speaker": segment.get("speaker", "SPEAKER_UNKNOWN"),
            "start": segment.get("start", -1.0),
            "end": segment.get("end", -1.0),
            "text": segment.get("text", None),
        }
        text_list.append(item)

        texts += segment.get("text", "") + "\n"

    text = json.dumps(text_list, ensure_ascii=False, indent=2)

    logger.info(f"회의 내용 : \n{texts}")

    summary = generate_summary(texts)

    logger.info(f"요약 내용 : \n{summary}")

    if os.path.exists(f"./tmp/{recording.filename}"):
        shutil.rmtree(f"./tmp/recordings/{recording.videoConferenceId}")

    return {
        "videoConferenceId": recording.videoConferenceId,
        "transcript": text,
        "summary": summary,
        "turnaround": str(datetime.now() - start_time),
    }
