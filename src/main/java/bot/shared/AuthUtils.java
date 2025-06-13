package bot.shared;

import bot.Secrets;
import java.util.Set;

/**
 * Класс проверки id пользователя. Доступ к @DEV_ITIS_FAQ_BOT
 * должны иметь только три разработчика, id которых лежат в "main/java/Secrets.java".
 * @author github.com/SlavikJunior
 * @version 1.0.1
 * @since 1.0.0
 **/

public class AuthUtils {
    private static final Set<Long> ALLOWED_DEV_IDS;

    static {
        ALLOWED_DEV_IDS = Secrets.getAllowedDevIds();
    }
    public static boolean isDeveloper(long userId) {
        return ALLOWED_DEV_IDS.contains(userId);
    }
}