package bot;

import bot.handlers.ITIScallbackHandler;
import bot.handlers.ITISmessageHandler;
import bot.shared.MessageStorage;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.List;

/**
 * Класс описывает @ITIS_FAQ_BOT.
 * @author github.com/SlavikJunior, github.com/tensaid7
 * @version 1.0.1
 * @since 1.0.0
 **/

public class ITISbot implements LongPollingUpdateConsumer {
    private final TelegramClient client;
    private final ITISmessageHandler ITIS_MESSAGE_HANDLER;
    private final ITIScallbackHandler ITIS_CALLBACK_HANDLER;
    private final MessageStorage MESSAGE_STORAGE;

    public ITISbot(DevLoggerBot loggerBot) {
        this.client = new OkHttpTelegramClient(Secrets.TOKEN);
        MESSAGE_STORAGE = new MessageStorage();
        this.ITIS_MESSAGE_HANDLER = new ITISmessageHandler(client, loggerBot, MESSAGE_STORAGE);
        this.ITIS_CALLBACK_HANDLER = new ITIScallbackHandler(client, loggerBot, MESSAGE_STORAGE);
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage()) {
                ITIS_MESSAGE_HANDLER.handle(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                ITIS_CALLBACK_HANDLER.handle(update.getCallbackQuery());
            }
        }
    }
}