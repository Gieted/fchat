package pl.pawelkielb.fchat.client;

import java.nio.file.Path;

public abstract class ExceptionHandler {
    private static void printError(String message) {
        System.err.println(message);
    }

    private static void checkDevMode() {
        if (Main.DEV_MODE) {
            throw new RuntimeException();
        }
    }

    public static void onClientPropertiesNotFound() {
        checkDevMode();
        printError("Cannot find fchat.properties. Are you in fchat's directory?");
        System.exit(1);
    }

    public static void onCommandNotUsedInChannelDirectory() {
        checkDevMode();
        printError("This command can be only used in channel directory");
        System.exit(2);
    }

    public static void onMessageNotProvided() {
        checkDevMode();
        printError("Please provide a message");
        System.exit(3);
    }

    public static void onInitCalledInFchatDirectory() {
        checkDevMode();
        printError("This is already an fchat directory");
        System.exit(4);
    }

    public static void onCommandUsedInChannelDirectory() {
        checkDevMode();
        printError("This command can only be used in fchat's root directory");
        System.exit(5);
    }

    public static void onCannotWriteFile(Path path) {
        checkDevMode();
        printError(String.format("Cannot write a file (%s)", path.toAbsolutePath()));
        System.exit(6);
    }

    public static void onCannotReadFile(Path path) {
        checkDevMode();
        printError(String.format("Cannot read a file (%s)", path.toAbsolutePath()));
        System.exit(7);
    }

    public static void onNetworkException() {
        checkDevMode();
        printError("There was an error while sending data");
        System.exit(8);
    }

    public static void onIllegalArgument(String message) {
        checkDevMode();
        printError(message);
        System.exit(9);
    }
}
