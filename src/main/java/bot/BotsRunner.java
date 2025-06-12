package bot;

import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Класс отвечает за запуск ботов.
 *
 * @author github.com/SlavikJunior
 * @version 1.0.1
 * @since 1.0.0
 **/

public class BotsRunner {

    private static final TelegramBotsLongPollingApplication APP = new TelegramBotsLongPollingApplication();

    public static void main(String[] args) {
//        setUpFastAPIappURL();

        System.out.println("\uD83D\uDE80 Начинаем процесс регистрации ботов!");
        // Создаем логгер-бот
        DevLoggerBot loggerBot = new DevLoggerBot();
        // Создаем основной бот и передаем ему логгер
        ITISbot itisBot = new ITISbot(loggerBot);

        try (BotSession devSession = APP.registerBot(Secrets.DEV_TOKEN, loggerBot);
             BotSession itisSession = APP.registerBot(Secrets.TOKEN, itisBot)) {
            System.out.println("✅ Оба бота запущены!");

        } catch (TelegramApiException e) {
            System.err.println("❌ Ошибка запуска ботов: " + e.getMessage());
        }
    }

//    private static void setUpFastAPIappURL() {
//        String ipRegex = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?):(0|[1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5])$";
//        String urlRegex = "^(?:https?:\\/\\/)?(?:localhost|(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,})(?::(0|[1-9]\\d{0,3}|[1-5]\\d{4}|6[0-4]\\d{3}|65[0-4]\\d{2}|655[0-2]\\d|6553[0-5]))?$";
//        Pattern ipPattern = Pattern.compile(ipRegex);
//        Pattern urlPattern = Pattern.compile(urlRegex);
//
//        String URL;
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.print("\uD83D\uDCAC Введите адрес:порт на котором работает FastAPI модуль: ");
//        do {
//            String input = scanner.nextLine();
//
//            Matcher ipMatcher = ipPattern.matcher(input);
//            Matcher urlMatcher = urlPattern.matcher(input);
//
//            if (ipMatcher.find() || urlMatcher.find()) {
//                URL = input;
//                break;
//            }
//            System.out.println("Вы можете ввести <ip:port> или <URL:port>");
//        } while (true);
//
//        URL = "http://" + URL;
//        URL = URL + "/ask";
//        FAQclient.setUpURL(URL);
//    }
}