package com.crewvy.workforce_service.performance.constant;

public enum Grade {
    A_PLUS("A+", 5),  // 점수를 정수로 변경
    A("A", 4),
    B_PLUS("B+", 3),
    B("B", 2),
    F("F", 0);

    private final String letter;
    private final int score; // 데이터 타입을 double에서 int로 변경

    Grade(String letter, int score) {
        this.letter = letter;
        this.score = score;
    }

    public String getLetter() {
        return letter;
    }

    public int getScore() {
        return score;
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
}