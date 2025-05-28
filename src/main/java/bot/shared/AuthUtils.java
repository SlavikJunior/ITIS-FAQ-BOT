package bot.shared;

import bot.Secrets;

import java.util.Set;

public class AuthUtils {
    private static final Set<Long> ALLOWED_DEV_IDS =
            Set.of(Long.parseLong(Secrets.DEV_ID_1), Long.parseLong(Secrets.DEV_ID_2));

    public static boolean isDeveloper(long userId) {
        return ALLOWED_DEV_IDS.contains(userId);
    }
}
