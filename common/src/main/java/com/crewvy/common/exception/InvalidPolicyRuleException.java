package com.crewvy.common.exception;

/**
 * 정책 규칙이 잘못되었을 때 발생하는 예외
 */
public class InvalidPolicyRuleException extends RuntimeException {
    public InvalidPolicyRuleException(String message) {
        super(message);
    }
}
