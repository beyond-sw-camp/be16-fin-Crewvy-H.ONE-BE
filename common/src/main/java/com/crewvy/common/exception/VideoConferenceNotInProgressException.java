
package com.crewvy.common.exception;

public class VideoConferenceNotInProgressException extends RuntimeException {

    public VideoConferenceNotInProgressException(String message) {
        super(message);
    }

    public VideoConferenceNotInProgressException(String message, Throwable cause) {
        super(message, cause);
    }
}
