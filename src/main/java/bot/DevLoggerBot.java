package bot;

import bot.handlers.DEVcallbackHandler;
import bot.handlers.DEVmessageHandler;
import bot.shared.LogEntry;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Класс описывает маршрутизацию и хранение логов @DEV_ITIS_FAQ_BOT.
 * @author github.com/SlavikJunior, github.com/tensaid7
 * @version 1.0.1
 * @since 1.0.0
 **/

public class DevLoggerBot implements LongPollingUpdateConsumer {
    private final TelegramClient CLIENT;
    private final DEVmessageHandler MESSAGE_HANDLER;
    private final DEVcallbackHandler CALLBACK_HANDLER;
    private final List<LogEntry> logs = new CopyOnWriteArrayList<>(); // потоко безопасное хранение

    public DevLoggerBot() {
        CLIENT = new OkHttpTelegramClient(Secrets.DEV_TOKEN);
        MESSAGE_HANDLER = new DEVmessageHandler(CLIENT, this);
        CALLBACK_HANDLER = new DEVcallbackHandler(CLIENT, this);
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasCallbackQuery()) {
                CALLBACK_HANDLER.handle(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                MESSAGE_HANDLER.handle(update.getMessage());
            }
        }
    }

    public void addLog(LogEntry log) {
        logs.add(log);
        if (logs.size() > 1000) logs.remove(0);
    }

    public List<LogEntry> getLogs(int limit) {
        return logs.stream()
                .sorted(java.util.Comparator.comparing(LogEntry::getTimestamp).reversed())
                .limit(limit)
                .toList();
    }
}