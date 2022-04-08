package pl.pawelkielb.fchat.client;

public class Console {
    public void println() {
        System.out.println();
    }

    public void println(String message) {
        System.out.println(message);
    }

    private int currentLineHash = 0;

    public void updateLine(String content) {
        int contentHash = content.hashCode();
        if (contentHash != currentLineHash) {

            System.out.print("\r" + content);
            currentLineHash = contentHash;
        }
    }
}
