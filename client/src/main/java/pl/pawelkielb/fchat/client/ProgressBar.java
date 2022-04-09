package pl.pawelkielb.fchat.client;

// \ =>              |5%
public class ProgressBar {
    private final static int length = 40;
    private final static double spinnerRotationsPerSecond = 8;

    private final Console console;

    public ProgressBar(Console console) {
        this.console = console;
    }

    private int spinnerPosition = 0;
    private long nextSpinTime = 0;

    public void update(double progress) {
        String spinner = switch (spinnerPosition % 4) {
            case 0 -> "|";
            case 1 -> "\\";
            case 2 -> "-";
            case 3 -> "/";
            default -> throw new AssertionError();
        };

        int progressPercentage = (int) (progress * 100);

        String progressBar = "=".repeat((int) (progress * (length - 2)) + 1) + ">";
        progressBar += " ".repeat(length - progressBar.length()) + "|";
        progressBar += progressPercentage + "%";
        console.updateLine(spinner + " " + progressBar);

        long currentTime = System.currentTimeMillis();

        if (currentTime > nextSpinTime) {
            spinnerPosition++;
            nextSpinTime = currentTime + (long) (1000 / spinnerRotationsPerSecond);
        }
    }
}
