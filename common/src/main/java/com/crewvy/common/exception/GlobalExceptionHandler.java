
package com.crewvy.common.exception;

import com.crewvy.common.dto.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 수행 중 발생하는 예외를 처리합니다.
     * @param e BusinessException
     * @return HTTP 400 (Bad Request) 상태 코드와 에러 메시지를 담은 ApiResponse
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(PermissionDeniedException.class)
    protected ResponseEntity<ApiResponse<?>> handlePermissionDeniedException(PermissionDeniedException e) {
        log.warn("PermissionDeniedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ApiResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * 처리되지 않은 모든 예외를 처리합니다.
     * @param e Exception
     * @return HTTP 500 (Internal Server Error) 상태 코드와 표준 에러 메시지를 담은 ApiResponse
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> notValidException(MethodArgumentNotValidException e) {
        String errMessage = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errMessage));
    }

    @ExceptionHandler(UserNotInvitedException.class)
    protected ResponseEntity<ApiResponse<?>> handleUserNotInvitedException(UserNotInvitedException e) {
        log.warn("UserNotInvitedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(UserNotHostException.class)
    protected ResponseEntity<ApiResponse<?>> handleUserNotHostException(UserNotHostException e) {
        log.warn("UserNotHostException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("MethodArgumentTypeMismatchException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("파라미터 형식이 올바르지 않습니다."));
    }

    @ExceptionHandler(VideoConferenceNotInProgressException.class)
    protected ResponseEntity<ApiResponse<?>> handleVideoConferenceNotInProgressException(VideoConferenceNotInProgressException e) {
        log.warn("VideoConferenceNotInProgressException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(VideoConferenceNotWaitingException.class)
    protected ResponseEntity<ApiResponse<?>> handleVideoConferenceNotWaitingException(VideoConferenceNotWaitingException e) {
        log.warn("VideoConferenceNotWaitingException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    protected ResponseEntity<ApiResponse<?>> handleEntityNotFoundException(EntityNotFoundException e) {
        log.warn("EntityNotFoundException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException e) {
        log.warn("RuntimeException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(UserAlreadyJoinedException.class)
    protected ResponseEntity<ApiResponse<?>> handleUserAlreadyJoinedException(UserAlreadyJoinedException e) {
        log.warn("UserAlreadyJoinedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(InvalidSenderException.class)
    protected ResponseEntity<ApiResponse<?>> handleInvalidSenderException(InvalidSenderException e) {
        log.warn("InvalidSenderException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ChatSendFailedException.class)
    protected ResponseEntity<ApiResponse<?>> handleChatSendFailedException(ChatSendFailedException e) {
        log.warn("ChatSendFailedException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(LiveKitClientException.class)
    protected ResponseEntity<ApiResponse<?>> handleLiveKitClientException(LiveKitClientException e) {
        log.warn("LiveKitClientException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(e.getMessage()));
    }
}
