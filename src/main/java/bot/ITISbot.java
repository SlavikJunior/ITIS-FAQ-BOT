package bot;

import bot.handlers.MessageHandler;
import bot.shared.FAQmodel;
import bot.shared.LogEntry;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.List;

public class ITISbot implements LongPollingUpdateConsumer {
    private final TelegramClient CLIENT;
    private final MessageHandler MESSAGE_HANDLER;
    private final DevLoggerBot LOGGER_BOT;
    private final FAQmodel FAQmodel;

    public ITISbot(DevLoggerBot loggerBot) {
        this.CLIENT = new OkHttpTelegramClient(Secrets.TOKEN);
        this.LOGGER_BOT = loggerBot;
        this.FAQmodel = new FAQmodel("path/to/model.bin");
        this.MESSAGE_HANDLER = new MessageHandler(CLIENT);
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String question = update.getMessage().getText();
                long userId = update.getMessage().getFrom().getId();
                long chatId = update.getMessage().getChatId();

                // Получаем ответ от модели (заглушка)
                // наверное будет приходить какой-то объект и в нём будет
                // и ответ и уверенность, не надо будет опрашивать два раза
                String answer = FAQmodel.getAnswer(question);
//                double confidence = FAQmodel.getConfidence(question);
                double confidence = 0.5;

                // Логируем проблемные ответы
                if (confidence < 0.7) {
                    LogEntry log = new LogEntry(
                            userId,
                            chatId,
                            question,
                            answer,
                            confidence,
                            Instant.now(),
                            "LOW_CONFIDENCE"
                    );
                    LOGGER_BOT.addLog(log);
                }

                // Отправляем ответ пользователю
                MESSAGE_HANDLER.sendAnswer(chatId, answer);
            }
        }
    }
}