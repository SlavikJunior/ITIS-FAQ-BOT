package bot;

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotsRunner {

    private static final TelegramBotsLongPollingApplication APP = new TelegramBotsLongPollingApplication();

    public static void main(String[] args) {
        try {
            System.out.println("\uD83D\uDE80 Начинаем процесс регистрации ботов!");
            // Создаем логгер-бот
            DevLoggerBot loggerBot = new DevLoggerBot();
            APP.registerBot(Secrets.DEV_TOKEN, loggerBot);

            // Создаем основной бот и передаем ему логгер
            ITISbot itisBot = new ITISbot(loggerBot);
            APP.registerBot(Secrets.TOKEN, itisBot);

            System.out.println("✅ Оба бота запущены!");
        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка запуска ботов: " + e.getMessage());
        }
    }
}