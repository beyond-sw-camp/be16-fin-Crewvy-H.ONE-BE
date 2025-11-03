from uuid import UUID
from pydantic import BaseModel
from enum import Enum

class TranscribeReq(BaseModel):
    videoConferenceId: UUID
    filename: str
    url: str
    bytes: int
    duration: int

class TranscribeRes(BaseModel):
    videoConferenceId: UUID
    transcript: str
    summary: str
    errorMsg: str
    status: str

class TranscribeStatus(Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"