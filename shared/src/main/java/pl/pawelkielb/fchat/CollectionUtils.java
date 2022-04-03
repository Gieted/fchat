package pl.pawelkielb.fchat;

import java.util.List;

public abstract class CollectionUtils {
    public static <T> T lastOrNull(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }

        return list.get(list.size() - 1);
    }
}
