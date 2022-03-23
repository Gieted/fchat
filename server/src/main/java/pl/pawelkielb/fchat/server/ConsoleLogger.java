package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.Logger;

public class ConsoleLogger implements Logger {

    private int i = 1;

    @Override
    public void info(String message) {
        System.out.printf("%d: %s\n", i++, message);
    }
}
