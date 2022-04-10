package pl.pawelkielb.fchat.exceptions;

public class PacketDecodeException extends RuntimeException {
    public PacketDecodeException(String message) {
        super(message);
    }
}
