
package com.crewvy.member_service.common.exception;

/**
 * 비즈니스 로직 상의 에러를 나타내기 위한 공통 예외 클래스
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
