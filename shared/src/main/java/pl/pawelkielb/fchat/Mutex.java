package pl.pawelkielb.fchat;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class Mutex {
    private final Queue<Runnable> queue = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public void lock(Runnable fn) {
        if (!lock.isLocked() && lock.tryLock()) {
            fn.run();
        } else {
            queue.add(fn);
        }
    }

    public void unlock() {
        if (lock.isLocked()) {
            Runnable next = queue.poll();
            if (next != null) {
                next.run();
            } else {
                lock.unlock();
            }
        }
    }
}
