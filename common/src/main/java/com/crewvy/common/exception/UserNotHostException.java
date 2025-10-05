
package com.crewvy.common.exception;

public class UserNotHostException extends RuntimeException {

    public UserNotHostException(String message) {
        super(message);
    }

    public UserNotHostException(String message, Throwable cause) {
        super(message, cause);
    }
}
