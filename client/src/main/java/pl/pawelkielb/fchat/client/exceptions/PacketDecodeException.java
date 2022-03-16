package pl.pawelkielb.fchat.client.exceptions;

public class PacketDecodeException extends RuntimeException {
    public PacketDecodeException(String message) {
        super(message);
    }
}
