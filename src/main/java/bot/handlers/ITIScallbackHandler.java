package bot.handlers;

import bot.DevLoggerBot;
import bot.Secrets;
import bot.shared.LogEntry;
import bot.shared.MessageStorage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * Класс представляет собой интерфейс callback взаимодействия @ITIS_FAQ_BOT
 * @author github.com/SlavikJunior
 * @version 1.0.1
 * @since 1.0.1
 **/

public class ITIScallbackHandler {
    private final TelegramClient CLIENT;
    private final DevLoggerBot LOGGER_BOT;
    private final MessageStorage MESSAGE_STORAGE;

    public ITIScallbackHandler(TelegramClient client, DevLoggerBot loggerBot, MessageStorage messageStorage) {
        CLIENT = client;
        LOGGER_BOT = loggerBot;
        MESSAGE_STORAGE = messageStorage;
    }

    public void handle(CallbackQuery callbackQuery) {
        User user = callbackQuery.getFrom();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        if (callbackQuery.getData().equals("admission_answer") && Secrets.isAdmission(user.getUserName())) {

        }

        // если заданный вопрос не принадлежит тому, кто фидбечит, просто игнорим
        if (!MESSAGE_STORAGE.isAsked(user.getId())) {
            return;
        }

        String[] data = callbackQuery.getData().split(":");
        if (data.length != 2 || !data[0].equals("feedback")) return;

        MessageStorage.QuestionInfo info = MESSAGE_STORAGE.get(messageId);
        if (info == null) return;

        String feedbackType = data[1];
        String question = info.getQuestion();
        Message message = (Message) callbackQuery.getMessage();
        String answer = message.getText();

        if (feedbackType.equals("no")) {
            handleNegativeFeedback(user.getId(), chatId, question, answer);
            sendNegativeFeedbackAlert(chatId, question);
        }

        removeFeedbackButtons(chatId, messageId);
        sendFeedbackConfirmation(chatId, feedbackType);
        MESSAGE_STORAGE.remove(messageId);
    }

    private void handleNegativeFeedback(long userId, long chatId, String question, String answer) {
        LOGGER_BOT.addLog(new LogEntry(
                userId, chatId, question, answer, 0.0,
                "Пользователь отметил ответ как неполезный", "BAD_FEEDBACK"
        ));
    }

    private void sendNegativeFeedbackAlert(long chatId, String question) {
        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("🚨 Негативный отзыв на вопрос:\n\n❓ Вопрос:\n" + question +
                            "\n\n\uD83D\uDCACПриемная комиссия: " + String.join(" ", Secrets.getAdmission()))
                    .build());
        } catch (Exception e) {
            System.err.println("Ошибка отправки алерта: " + e.getMessage());
        }
    }

    private void removeFeedbackButtons(long chatId, int messageId) {
        try {
            CLIENT.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .replyMarkup(null)
                    .build());
        } catch (Exception e) {
            System.out.println("Ошибка удаления кнопок: " + e.getMessage());
        }
    }

    private void sendFeedbackConfirmation(long chatId, String feedbackType) {
        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(feedbackType.equals("yes")
                            ? "Спасибо за отзыв! \uD83D\uDC4D"
                            : "Мы учтем ваш отзыв и улучшим ответ! \uD83D\uDCDD")
                    .build());
        } catch (Exception e) {
            System.err.println("Ошибка отправки подтверждения: " + e.getMessage());
        }
    }
}