package bot;

import bot.handlers.MessageHandler;
import bot.shared.FAQmodel;
import bot.shared.LogEntry;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

public class ITISbot implements LongPollingUpdateConsumer {
    private final TelegramClient CLIENT;
    private final MessageHandler MESSAGE_HANDLER;
    private final DevLoggerBot LOGGER_BOT;
    private final FAQmodel FAQmodel;
    private final double LOW_CONFIDENCE = 0.7;

    public ITISbot(DevLoggerBot loggerBot) {
        this.CLIENT = new OkHttpTelegramClient(Secrets.TOKEN);
        this.LOGGER_BOT = loggerBot;
        this.FAQmodel = new FAQmodel("path/to/model.bin");
        this.MESSAGE_HANDLER = new MessageHandler(CLIENT);
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage()) {
                User user = update.getMessage().getFrom();
                if (Secrets.isAlarmUser(String.valueOf(user.getId()))) {
//                    CLIENT.execute(какойто executable или BotApiMethod)
                    MESSAGE_HANDLER.sendMessage(update.getMessage().getChatId(), "\uD83D\uDEA8 Мы не обрабатываем сообщения от жуков \uD83E\uDEB5");
                    continue;
                }
            }

            String text = "";
            if (update.hasMessage() && update.getMessage().hasText()) {
                text = update.getMessage().getText();
                long userId = update.getMessage().getFrom().getId();
                long chatId = update.getMessage().getChatId();

                if (text.equals("/start") || text.equals("/start@ITIS_FAQ_BOT"))
                    MESSAGE_HANDLER.sendMessage(chatId, "\uD83E\uDD16 FAQ-бот приёмной комиссии. Помогаю абитуриентам поступить!");
                else if (text.equals("/help") || text.equals("/help@ITIS_FAQ_BOT"))
                    MESSAGE_HANDLER.sendMessage(chatId, "\uD83D\uDCA1 Попробуй /ask и напиши свой вопросик");
                else if (text.equals("/ask") || text.equals("/ask@ITIS_FAQ_BOT")) {
                    MESSAGE_HANDLER.sendMessage(chatId, "\uD83D\uDEA8 Мы не обрабатываем пустые запросы");
                } else if (text.startsWith("/ask ") || text.startsWith("/ask@ITIS_FAQ_BOT ")) {
                    // Получаем ответ от модели (заглушка)
                    // наверное будет приходить какой-то объект и в нём будет
                    // и ответ и уверенность, не надо будет опрашивать два раза
                    String answer = FAQmodel.getAnswer(text);
                    double confidence = FAQmodel.getConfidence(text);

                    // Логируем проблемные ответы
                    handleConfidence(confidence, userId, chatId, text, answer);

                    // Отправляем ответ пользователю
                    MESSAGE_HANDLER.sendAnswer(chatId, answer);
                }
            } else if (update.hasCallbackQuery()) {
                handleFeedback(update.getCallbackQuery(), text);
            }
        }
    }

    private void handleConfidence(double confidence, long userId, long chatId, String question, String answer) {
        if (confidence < LOW_CONFIDENCE) {
            LogEntry log = new LogEntry(
                    userId,
                    chatId,
                    question,
                    answer,
                    confidence,
                    "Автоматически добавленный лог",
                    "LOW_CONFIDENCE"
            );
            LOGGER_BOT.addLog(log);
        }
    }

    private void handleFeedback(CallbackQuery callbackQuery, String question) {
        String[] data = callbackQuery.getData().split(":");
        if (data.length != 2 || !data[0].equals("feedback")) return;

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