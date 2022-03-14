package pl.pawelkielb.fchat.client;

public class ExceptionHandler {
    private static void printError(String message) {
        System.err.println(message);
    }
    
    public void onClientPropertiesNotFound() {
        printError("Cannot find fchat.properties. Are you in fchat's directory?");
        System.exit(1);
    }

    public void onCommandNotUsedInChannelDirectory() {
        printError("This command can be only used in channel directory");
        System.exit(2);
    }

    public void onMessageNotProvided() {
        printError("Please provide a message");
        System.exit(3);
    }
}
