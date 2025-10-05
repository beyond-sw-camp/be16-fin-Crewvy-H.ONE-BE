
package com.crewvy.common.exception;

public class VideoConferenceNotWaitingException extends RuntimeException {

    public VideoConferenceNotWaitingException(String message) {
        super(message);
    }

    public VideoConferenceNotWaitingException(String message, Throwable cause) {
        super(message, cause);
    }
}
