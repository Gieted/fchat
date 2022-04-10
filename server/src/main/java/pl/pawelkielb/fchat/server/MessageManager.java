package pl.pawelkielb.fchat.server;

import pl.pawelkielb.fchat.data.Message;

import java.util.UUID;

/**
 * A central entity to which all messages are pushed to.
 */
public class MessageManager {
    private final Database database;

    public MessageManager(Database database) {
        this.database = database;
    }

    public void pushMessage(UUID channel, Message message) {
        database.saveMessage(channel, message);
    }
}
