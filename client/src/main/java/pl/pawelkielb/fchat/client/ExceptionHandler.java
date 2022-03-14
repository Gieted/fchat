package pl.pawelkielb.fchat.client;

import java.nio.file.Path;

public class ExceptionHandler {
    private final boolean isDevModeEnabled;

    public ExceptionHandler(boolean isDevModeEnabled) {
        this.isDevModeEnabled = isDevModeEnabled;
    }

    private static void printError(String message) {
        System.err.println(message);
    }

    private void checkDevMode() {
        if (isDevModeEnabled) {
            throw new RuntimeException();
        }
    }

    public void onClientPropertiesNotFound() {
        checkDevMode();
        printError("Cannot find fchat.properties. Are you in fchat's directory?");
        System.exit(1);
    }

    public void onCommandNotUsedInChannelDirectory() {
        checkDevMode();
        printError("This command can be only used in channel directory");
        System.exit(2);
    }

    public void onMessageNotProvided() {
        checkDevMode();
        printError("Please provide a message");
        System.exit(3);
    }

    public void onCannotSaveClientProperties() {
        checkDevMode();
        printError("Cannot save client properties. Is the file opened by another program?");
        System.exit(4);
    }

    public void onInitCalledInFchatDirectory() {
        checkDevMode();
        printError("This is already an fchat directory");
        System.exit(5);
    }

    public void onCommandUsedInChannelDirectory() {
        checkDevMode();
        printError("This command can only be used in fchat's root directory");
        System.exit(6);
    }

    public void onCannotWriteFile(Path path) {
        checkDevMode();
        printError(String.format("Cannot write a file (%s)", path.toAbsolutePath()));
        System.exit(7);
    }

    public void onNetworkException() {
        checkDevMode();
        printError("There was an error while sending data");
        System.exit(8);
    }
}
