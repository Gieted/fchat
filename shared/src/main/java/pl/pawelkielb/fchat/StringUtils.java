package pl.pawelkielb.fchat;

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
}
