package com.crewvy.workforce_service.performance.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum Grade {
    A_PLUS("A+", 5),  // 점수를 정수로 변경
    A("A", 4),
    B_PLUS("B+", 3),
    B("B", 2),
    F("F", 0);

    private final String letter;
    @Getter
    private final int score; // 데이터 타입을 double에서 int로 변경

    Grade(String letter, int score) {
        this.letter = letter;
        this.score = score;
    }

    @JsonValue
    public String getLetter() {
        return letter;
    }

    // DB에서 읽어온 정수 점수로 해당하는 Grade를 찾아주는 메서드
    public static Grade fromScore(int score) {
        for (Grade grade : values()) {
            if (grade.score == score) {
                return grade;
            }
        }
        throw new IllegalArgumentException("Invalid score: " + score);
    }

    @JsonCreator
    public static Grade fromLetter(String letter) {
        for (Grade grade : Grade.values()) {
            if (grade.letter.equalsIgnoreCase(letter)) {
                return grade;
            }
        }
        // 일치하는 것이 없으면 예외 발생 (또는 null 반환)
        throw new IllegalArgumentException("Unknown grade letter: " + letter);
    }
}