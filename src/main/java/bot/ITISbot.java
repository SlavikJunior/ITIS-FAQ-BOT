package bot;

import bot.handlers.MessageHandler;
import bot.shared.FAQmodel;
import bot.shared.LogEntry;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

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
            String question = "";
            if (update.hasMessage() && update.getMessage().hasText()) {
                question = update.getMessage().getText();
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
                handleFeedback(update.getCallbackQuery(), question);
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
                    "",
                    "LOW_CONFIDENCE"
            );
            LOGGER_BOT.addLog(log);
        }
    }

    private void handleFeedback(CallbackQuery callbackQuery, String question) {
        String[] data = callbackQuery.getData().split(":");
        if (data.length != 3 || !data[0].equals("feedback")) return;

        String feedbackType = data[1];
        Message maybeInaccessibleMessage = (Message) callbackQuery.getMessage();

        String answer = maybeInaccessibleMessage.getText(); // Получаем оригинальный вопрос

        long chatId = callbackQuery.getMessage().getChatId();

        if (feedbackType.equals("no")) {
            LogEntry log = new LogEntry(
                    callbackQuery.getFrom().getId(),
                    callbackQuery.getMessage().getChatId(),
                    question,
                    answer, // Сохраняем полный текст вопроса
                    0.0,
                    "Пользователь отметил ответ как неполезный",
                    "BAD_FEEDBACK"
            );
            LOGGER_BOT.addLog(log);

            // Отправляем уведомление
            sendNegativeFeedbackAlert(callbackQuery.getMessage().getChatId(), answer);
        }

        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(feedbackType.equals("yes") ? "Спасибо за отзыв! \uD83D\uDCA1" : "Мы учтем ваш отзыв и улучшим ответ! \uD83D\uDCDD")
                    .build());
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки подтверждения: " + e.getMessage());
        }
    }

    public void sendNegativeFeedbackAlert(long chatId, String question) {
        String message = "🚨 Негативный отзыв на вопрос:\n\n" +
                "❓ Вопрос:\n" + question + "\n\n" +
                "\uD83D\uDCACПриемная комиссия: " + String.join(" ", Secrets.getAdmission());

        SendMessage alert = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build();

        try {
            CLIENT.execute(alert);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки алерта: " + e.getMessage());
        }
    }
}