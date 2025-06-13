package bot.handlers;

import bot.DevLoggerBot;
import bot.Secrets;
import bot.shared.FAQclient;
import bot.shared.LogEntry;
import bot.shared.MessageStorage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс представляет собой интерфейс текстового взаимодействия @ITIS_FAQ_BOT
 *
 * @author github.com/SlavikJunior
 * @version 1.0.1
 * @since 1.0.1
 **/

public class ITISmessageHandler {
    private final TelegramClient CLIENT;
    private final DevLoggerBot LOGGER_BOT;
    private final MessageStorage MESSAGE_STORAGE;

    public ITISmessageHandler(TelegramClient client, DevLoggerBot loggerBot, MessageStorage messageStorage) {
        CLIENT = client;
        LOGGER_BOT = loggerBot;
        MESSAGE_STORAGE = messageStorage;
    }

    public void handle(Message message) {
        if (!message.hasText()) return;

        User user = message.getFrom();
        String userName = "@" + user.getUserName();
        String text = message.getText();
        long userId = user.getId();
        long chatId = message.getChatId();

        // обрабатываем действия администрации
        if (Secrets.isAdmission(userName) && MESSAGE_STORAGE.isAdminResponding(user.getId())) {
            handleAdminResponse(message);
            return;
        }

        // обрабатываем действия особых пользователей
        if (Secrets.isAlarmUser(String.valueOf(userId))) {
            sendMessage(chatId, Secrets.getAnswerForAlarmUser(String.valueOf(userId)));
            return;
        }

        if (text.equals("/start") || text.equals("/start@ITIS_FAQ_BOT")) {
            sendMessage(chatId, "\uD83E\uDD16 FAQ-бот приёмной комиссии. Помогаю абитуриентам поступить!");
        } else if (text.equals("/help") || text.equals("/help@ITIS_FAQ_BOT")) {
            sendMessage(chatId, "\uD83D\uDCCC Напиши /ask и задай вопрос");
        } else if (text.equals("/ask") || text.equals("/ask@ITIS_FAQ_BOT")) {
            sendMessage(chatId, "\uD83D\uDCA1 Попробуй /ask и через пробел напиши свой вопрос");
        } else if (text.startsWith("/ask ") || text.startsWith("/ask@ITIS_FAQ_BOT ")) {
            handleQuestion(message, text, userId, chatId);
        }
    }

    private void handleQuestion(Message message, String text, long userId, long chatId) {
        String question = text.startsWith("/ask ")
                ? text.replace("/ask ", "")
                : text.replace("/ask@ITIS_FAQ_BOT ", "");

        String answer = FAQclient.ask(question);
        answer = processAnswer(answer, userId, chatId, question);

        if (!answer.isEmpty()) {
            Message answerMessage = sendAnswer(chatId, answer);
            MESSAGE_STORAGE.put(answerMessage.getMessageId(), new MessageStorage.QuestionInfo(userId, question));
        } else
            // если модель не смогла ответить, сразу уведомляем администратора
            sendAdminResponseRequest(chatId, question, "", message.getFrom());
    }

    private String processAnswer(String answer, long userId, long chatId, String question) {
        if (answer.equals("LOW_CONFIDENCE")) {
            LOGGER_BOT.addLog(new LogEntry(
                    userId, chatId, question, answer, 0.0,
                    "Автоматически добавленный лог", "LOW_CONFIDENCE"
            ));
            return "";
        }
        return answer.replace("\"answer\":", "")
                .replace("\"", "")
                .replace("{", "")
                .replace("}", "");
    }

    public Message sendAnswer(long chatId, String answer) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .replyMarkup(createFeedbackButtons())
                .build();
        try {
            return CLIENT.execute(message);
        } catch (Exception e) {
            System.out.println("Ошибка отправки ответа: " + e.getMessage());
            return null;
        }
    }

    private InlineKeyboardMarkup createFeedbackButtons() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardRow row = new InlineKeyboardRow();

        row.add(InlineKeyboardButton.builder()
                .text("👍 Помогло")
                .callbackData("feedback:yes")
                .build());

        row.add(InlineKeyboardButton.builder()
                .text("👎 Не помогло")
                .callbackData("feedback:no")
                .build());

        rows.add(row);
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    void sendMessage(long chatId, String text) {
        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (Exception e) {
            System.out.println("Ошибка отправки сообщения");
        }
    }

    void sendAdminResponseRequest(long chatId, String question, String badAnswer, User user) {
        String messageText = buildAdminRequestText(question, badAnswer, user.getUserName());
        InlineKeyboardMarkup markup = createResponseButton();

        try {
            Message sentMessage = CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(messageText)
                    .replyMarkup(markup)
                    .build());

            MESSAGE_STORAGE.addPendingQuestion(
                    sentMessage.getMessageId(),
                    user.getId(),
                    user.getUserName(),
                    chatId
            );
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки запроса администратору");
        }
    }

    private String buildAdminRequestText(String question, String badAnswer, String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 Требуется ответ для @").append(username).append(":\n\n");
        sb.append("❓ Вопрос: ").append(question).append("\n\n");

        if (!badAnswer.isEmpty()) {
            sb.append("💬 Проблемный ответ: ").append(badAnswer).append("\n\n");
        }

        return sb.toString();
    }

    private InlineKeyboardMarkup createResponseButton() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDD0D Ответить")
                                .callbackData("admin_response")
                                .build()
                ))
                .build();
    }

    private void handleAdminResponse(Message message) {
        Integer questionMessageId = MESSAGE_STORAGE.getAdminMessageId(message.getFrom().getId());
        if (questionMessageId == null) return;

        MessageStorage.PendingQuestion question = MESSAGE_STORAGE.getPendingQuestion(questionMessageId);
        if (question == null) return;

        try {
            // Отправляем ответ
            String response = "✉ @" + question.username + ", ответ от приёмной комиссии:\n" + message.getText();
            CLIENT.execute(SendMessage.builder()
                    .chatId(question.chatId)
                    .text(response)
                    .build());

            // Полная очистка состояния
            MESSAGE_STORAGE.removePendingQuestion(questionMessageId);
            MESSAGE_STORAGE.clearAdminState(message.getFrom().getId());

        } catch (TelegramApiException e) {
            System.out.println("Ошибка обработки ответа");
        }
    }
}