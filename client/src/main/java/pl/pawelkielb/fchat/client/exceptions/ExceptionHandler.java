package pl.pawelkielb.fchat.client.exceptions;

import pl.pawelkielb.fchat.client.Database;
import pl.pawelkielb.fchat.client.Main;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class ExceptionHandler {
    private static void printError(String message) {
        System.err.println(message);
    }

    private static void checkDevMode() {
        if (Main.DEV_MODE) {
            throw new RuntimeException();
        }
    }

    private static String getExceptionPath(Exception e) {
        List<String> result = new ArrayList<>();
        Throwable throwable = e;
        while (throwable != null) {
            if (throwable.getMessage() != null) {
                result.add(throwable.getMessage());
            } else {
                result.add(throwable.getClass().getSimpleName());
            }
            throwable = throwable.getCause();
        }

        return String.join(" -> ", result);
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

    public static void onAlreadyInitialized() {
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

    public static void onNetworkException(IOException e) {
        checkDevMode();
        printError(getExceptionPath(new RuntimeException("There was an error while sending data", e)));
        System.exit(8);
    }

    public static void onIllegalArgument(String message, IllegalArgumentException e) {
        checkDevMode();
        printError(getExceptionPath(new RuntimeException(message, e)));
        System.exit(9);
    }

    public static void onIllegalArgument(String message) {
        onIllegalArgument(message, null);
    }

    public static void onCannotFindClientConfig() {
        checkDevMode();
        printError("Cannot find fchat.properties file. Are you in fchat's directory?");
        System.exit(10);
    }

    public static void onUnknownCommand(String command) {
        checkDevMode();
        printError("Unknown command: " + command);
        System.exit(11);
    }

    public static void onServerDisconnected() {
        checkDevMode();
        printError("Server unexpectedly disconnected");
        System.exit(12);
    }

    public static void onIllegalNameProvided() {
        checkDevMode();
        printError("Name may not contain commas, equals symbols and new line characters");
        System.exit(13);
    }

    public static void onInvalidClientConfig(Database.InvalidConfigException e) {
        checkDevMode();
        printError(getExceptionPath(new RuntimeException("Invalid client config", e)));
        System.exit(14);
    }

    public static void onInvalidChannelConfig(Database.InvalidConfigException e) {
        checkDevMode();
        printError(getExceptionPath(new RuntimeException("Invalid channel config", e)));
        System.exit(15);
    }

    public static void onUnknownException(Exception e) {
        checkDevMode();
        printError(getExceptionPath(new RuntimeException("Unknown error happened", e)));
        System.exit(16);
    }
}
