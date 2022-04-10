package pl.pawelkielb.fchat;

import java.io.IOException;

public class NetworkException extends RuntimeException {
    public NetworkException(IOException cause) {
        super(cause.getMessage());
    }
}
