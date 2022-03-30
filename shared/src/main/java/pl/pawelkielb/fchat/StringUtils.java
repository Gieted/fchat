package pl.pawelkielb.fchat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StringUtils {
    public static String increment(String string) {
        Pattern pattern = Pattern.compile("(.*)\\((\\d+)\\)$");
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) {
            int current = Integer.parseInt(matcher.group(2));
            return matcher.group(1) + "(" + ++current + ")";
        } else {
            return string + " (1)";
        }
    }

    public static String incrementFileName(String string) {
        List<String> stringSplit = Arrays.asList(string.split("\\."));
        if (stringSplit.size() == 1) {
            return increment(string);
        }

        String fileName = String.join(".", stringSplit.subList(0, stringSplit.size() - 1));
        String extension = stringSplit.get(stringSplit.size() - 1);

        return increment(fileName) + "." + extension;
    }
}
