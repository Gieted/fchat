package pl.pawelkielb.fchat;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private final List<Runnable> listeners = new ArrayList<>();

    public void subscribe(Runnable runnable) {
        listeners.add(runnable);
    }

    public void trigger() {
        listeners.forEach(Runnable::run);
    }
}
