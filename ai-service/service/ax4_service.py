import gc
import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
import logging

logger = logging.getLogger(__name__)


def generate_summary(text: str):
    logger.info("A.X-4 요약 시작")
    model_name = "skt/A.X-4.0-Light"
    model = AutoModelForCausalLM.from_pretrained(
        model_name,
        torch_dtype=torch.bfloat16,
        device_map="auto",
        cache_dir="./models/skt/",  # 원하는 경로 지정
    )

    model.eval()
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    messages = [
        {
            "role": "system",
            "content": """당신은 전문적인 회의록 요약 전문가입니다.
                        아래의 JSON 형식 회의 대화록을 분석하고, 다음의 요령에 따라 팀 회의 내용을 자연스럽게 요약하세요.

                        [요약 지침]
                        1. 논의한 전체 주제와 프로젝트 취지를 먼저 명확히 제시하세요.
                        2. 논의된 사회적 배경·문제 인식·시작 동기 등을 짧은 문장으로 설명하세요.
                        3. 기술적 접근(사용한 기술 스택, AI 및 시스템 구성)과 기능적 논의(어플리케이션의 주요 기능, 프로세스)를 핵심만 간결하게 정리하세요.
                        4. 기존 서비스/기술과의 차별점·경쟁력 논의가 있었다면 요약해서 포함하세요.
                        5. 회의 중 언급된 사용자의 기대 효과, 서비스 효과, 향후 발전 가능성 등은 마지막에 한 문단으로 요약하세요.
                        6. 팀원 역할 분담 또는 책임자가 명시되었으면 함께 정리하세요.
                        7. 원본 텍스트를 그대로 복사하거나 나열하지 말고, 회의의 흐름을 반영하여 재구성하세요.

                        출력 형식:
                        - JSON 내 모든 텍스트를 통합적으로 이해한 뒤, 하나의 완결된 회의 요약 문단을 한국어로 작성하세요.
                        - 문체는 회의 공식 요약(회의록 요약)처럼 객관적, 간결하게 유지하세요.""",
        },
        {"role": "user", "content": text},
    ]

    input_ids = tokenizer.apply_chat_template(
        messages, add_generation_prompt=True, return_tensors="pt"
    ).to(model.device)

    with torch.no_grad():
        output = model.generate(
            input_ids,
            max_new_tokens=512,
            do_sample=False,
        )

    len_input_prompt = len(input_ids[0])
    response = tokenizer.decode(output[0][len_input_prompt:], skip_special_tokens=True)

    gc.collect()
    torch.cuda.empty_cache()

    logger.info("A.X-4 요약 완료")
    return response
