package bot;

import bot.handlers.DevCommandHandler;
import bot.shared.*;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.*;

public class DevLoggerBot implements LongPollingUpdateConsumer {

    private final List<LogEntry> LOGS = new ArrayList<>();
    private final TelegramClient DEVELOPER_CLIENT = new OkHttpTelegramClient(Secrets.DEV_TOKEN);
    private final DevCommandHandler COMMAND_HANDLER = new DevCommandHandler(DEVELOPER_CLIENT, this);

    public void sendAccessDenied(Long chatId) {
        SendMessage msg = new SendMessage(chatId.toString(), "🚫 Доступ только для разработчиков!");
        try {
            DEVELOPER_CLIENT.execute(msg);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка при ответе на входящий запрос от того, кто не имеет доступа!");
        }
    }

    public void addLog(LogEntry log) {
        LOGS.add(log);
        if (LOGS.size() > 1000) LOGS.removeFirst(); // Ограничиваем размер
    }

    public List<LogEntry> getLogs(int limit) {
        return LOGS.stream()
                .sorted(Comparator.comparing(LogEntry::getTimestamp).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            }

            if (!update.hasMessage() || !update.getMessage().hasText()) continue;

            User user = update.getMessage().getFrom();
            if (!AuthUtils.isDeveloper(user.getId())) {
                sendAccessDenied(update.getMessage().getChatId());
                continue;
            }

            COMMAND_HANDLER.handle(update.getMessage());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (data.startsWith("/add") || data.startsWith("/get") || data.startsWith("/remove"))
            COMMAND_HANDLER.handleCallbackFromAdminPanel(callbackQuery);
//            COMMAND_HANDLER.sendMessageWithAdminPanel(callbackQuery.getMessage().getChatId(), String.valueOf(callbackQuery.getFrom().getId()));
    }
}
