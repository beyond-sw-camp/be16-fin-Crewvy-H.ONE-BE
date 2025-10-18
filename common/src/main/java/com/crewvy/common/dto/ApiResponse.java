
package com.crewvy.common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    @JsonCreator
    public ApiResponse(
            @JsonProperty("success") boolean success,
            @JsonProperty("data") T data,
            @JsonProperty("message") String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

//    public ApiResponse(boolean success, T data, String message) {
//        this.success = success;
//        this.data = data;
//        this.message = message;
//    }

    // 성공 응답 (데이터와 메시지 포함)
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    // 성공 응답 (데이터만 포함)
    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
