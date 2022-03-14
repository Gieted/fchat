package pl.pawelkielb.fchat.client;

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
}
