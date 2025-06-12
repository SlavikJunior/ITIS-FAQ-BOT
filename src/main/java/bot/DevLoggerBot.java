package bot;

import bot.handlers.DevCommandHandler;
import bot.shared.AuthUtils;
import bot.shared.LogEntry;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс описывает @DEV_ITIS_FAQ_BOT.
 * @author github.com/SlavikJunior, github.com/tensaid7
 * @version 1.0.0
 * @since 1.0.0
 **/

public class DevLoggerBot implements LongPollingUpdateConsumer {
    private final TelegramClient CLIENT;
    private final DevCommandHandler COMMAND_HANDLER;
    private final ConcurrentHashMap<Long, Long> messageToUserMap = new ConcurrentHashMap<>();
    private final List<LogEntry> LOGS = new ArrayList<>();

    public DevLoggerBot() {
        this.CLIENT = new OkHttpTelegramClient(Secrets.DEV_TOKEN);
        this.COMMAND_HANDLER = new DevCommandHandler(CLIENT, this);
    }

    public void addLog(LogEntry log) {
        LOGS.add(log);
        if (LOGS.size() > 1000)
            LOGS.removeFirst(); // Ограничиваем размер
    }

    public List<LogEntry> getLogs(int limit) {
        return LOGS.stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .limit(limit)
                .toList();
    }

    public void sendAccessDenied(Long chatId) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId.toString())
                .text("🚫 Доступ только для разработчиков!")
                .build();
        try {
            CLIENT.execute(msg);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения об отказе: " + e.getMessage());
        }
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            try {
                if (update.hasCallbackQuery()) {
                    // Все callback'и передаем в обработчик
                    COMMAND_HANDLER.handleAdminCallback(update.getCallbackQuery());
                    continue;
                }

                if (update.hasMessage() && update.getMessage().hasText()) {
                    Message message = update.getMessage();
                    User user = message.getFrom();

                    // Проверка прав один раз на входе
                    if (!AuthUtils.isDeveloper(user.getId())) {
                        sendAccessDenied(message.getChatId());
                        continue;
                    }

                    // Обработка текстовых команд
                    if (message.isCommand()) {
                        COMMAND_HANDLER.handle(message);
                    }

                    // Обработка ответов на запросы ID
                    else if (message.getReplyToMessage() != null && message.hasText() && message.getText().equals("Конец жучьему криминалу")) {
                        COMMAND_HANDLER.handleClearCommand(message);
                    }

                    else if (message.getReplyToMessage() != null) {
                        COMMAND_HANDLER.handleTextAfterCallback(message);
                    }
                }
            } catch (Exception e) {
                System.err.println("Ошибка обработки update: " + e.getMessage());
            }
        }
    }
}