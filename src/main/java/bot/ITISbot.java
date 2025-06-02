package bot;

import bot.handlers.MessageHandler;
import bot.shared.FAQmodel;
import bot.shared.LogEntry;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
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
                double confidence = FAQmodel.getConfidence(question);

                // Логируем проблемные ответы
                handleConfidence(confidence, userId, chatId, question, answer);

                // Отправляем ответ пользователю
                MESSAGE_HANDLER.sendAnswer(chatId, question, answer);
            } else if (update.hasCallbackQuery()) {

            }
        }
    }

    private void handleConfidence(double confidence, long userId, long chatId, String question, String answer) {
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
    }

    private void handleFeedback(CallbackQuery callbackQuery) {
        String[] data = callbackQuery.getData().split(":");
        if (data.length != 3 || !data[0].equals("feedback"))
            return;

        String feedbackType = data[1];
        int questionHash = Integer.parseInt(data[2]);
        long userId = callbackQuery.getFrom().getId();
        long chatId = callbackQuery.getMessage().getChatId();

        if (feedbackType.equals("no")) {
            LogEntry log = new LogEntry(
                    userId,
                    chatId,
                    "HASH:" + questionHash, // Сохраняем хеш вопроса
                    "", // Ответ уже есть в предыдущих логах
                    0.0, // Нулевая уверенность для негативных отзывов
                    Instant.now(),
                    "BAD_FEEDBACK"
            );
            LOGGER_BOT.addLog(log);
        }

        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(feedbackType.equals("yes") ? "Спасибо за отзыв!" : "Мы учтем ваш отзыв и улучшим ответ!")
                    .build());
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки подтверждения: " + e.getMessage());
        }
    }
}