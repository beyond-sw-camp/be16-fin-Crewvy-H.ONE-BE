
package com.crewvy.common.exception;

public class UserAlreadyJoinedException extends RuntimeException {

    public UserAlreadyJoinedException(String message) {
        super(message);
    }

    public UserAlreadyJoinedException(String message, Throwable cause) {
        super(message, cause);
    }
}
