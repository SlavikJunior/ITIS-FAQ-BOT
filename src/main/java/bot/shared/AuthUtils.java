package bot.shared;

import bot.Secrets;
import java.util.Set;

/**
 * Класс проверки id пользователя. Доступ к @DEV_ITIS_FAQ_BOT
 * должны иметь только три разработчика, id которых лежат в "main/java/Secrets.java".
 * @author github.com/SlavikJunior
 * @version 1.0.0
 * @since 1.0.0
 **/
public class AuthUtils {
    private static final Set<Long> ALLOWED_DEV_IDS = Set.of(
            Long.parseLong(Secrets.DEV_ID_1),
            Long.parseLong(Secrets.DEV_ID_2),
            Long.parseLong(Secrets.DEV_ID_3)
    );

    public static boolean isDeveloper(long userId) {
        return ALLOWED_DEV_IDS.contains(userId);
    }
}