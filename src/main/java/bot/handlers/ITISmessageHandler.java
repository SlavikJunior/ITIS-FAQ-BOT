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
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс представляет собой интерфейс текстового взаимодействия @ITIS_FAQ_BOT
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
        String text = message.getText();
        long userId = user.getId();
        long chatId = message.getChatId();

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
        } else {
            // не знание ответа: Тегает сотрудника, + "Вы можете дождаться ответа сотрудника, либо написать сами"
            // + кнопка ответить, которую могут нажать только сотрудники
            sendMessage(chatId, "🚨 Не могу ответить на вопрос:\n\n❓ Вопрос:\n" + question +
                    "\n\n\uD83D\uDCACПриемная комиссия: " + String.join(" ", Secrets.getAdmission()));
        }
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

    // сделать private потом
    public void sendMessage(long chatId, String text) {
        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (Exception e) {
            System.out.println("Ошибка отправки сообщения");
        }
    }
}