
package com.crewvy.common.exception;

public class UserNotInvitedException extends RuntimeException {

    public UserNotInvitedException(String message) {
        super(message);
    }

    public UserNotInvitedException(String message, Throwable cause) {
        super(message, cause);
    }
}
