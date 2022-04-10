package pl.pawelkielb.fchat.exceptions;

import java.io.IOException;

public class NetworkException extends RuntimeException {
    public NetworkException(IOException cause) {
        super(cause.getMessage());
    }
}
