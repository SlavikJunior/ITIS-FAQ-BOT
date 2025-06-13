package bot;

import bot.handlers.MessageHandler;
import bot.shared.FAQclient;
import bot.shared.LogEntry;
import bot.shared.MessageStorage;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.List;

/**
 * Класс описывает @ITIS_FAQ_BOT.
 * @author github.com/SlavikJunior, github.com/tensaid7
 * @version 1.0.0
 * @since 1.0.0
 **/

public class ITISbot implements LongPollingUpdateConsumer {
    private final TelegramClient CLIENT;
    private final MessageHandler MESSAGE_HANDLER;
    private final DevLoggerBot LOGGER_BOT;
    private final MessageStorage MESSAGE_STORAGE;

    public ITISbot(DevLoggerBot loggerBot) {
        CLIENT = new OkHttpTelegramClient(Secrets.TOKEN);
        LOGGER_BOT = loggerBot;
        MESSAGE_HANDLER = new MessageHandler(CLIENT);
        MESSAGE_STORAGE = new MessageStorage();
    }

    @Override
    public void consume(List<Update> updates) {
        for (Update update : updates) {
            if (update.hasMessage()) {
                User user = update.getMessage().getFrom();
                if (Secrets.isAlarmUser(String.valueOf(user.getId()))) {
                    MESSAGE_HANDLER.sendMessage(
                            update.getMessage().getChatId(),
                            Secrets.getAnswerForAlarmUser(
                                    String.valueOf(user.getId())));
                    continue;
                }
            }

            String text;
            if (update.hasMessage() && update.getMessage().hasText()) {
                text = update.getMessage().getText();
                long userId = update.getMessage().getFrom().getId();
                long chatId = update.getMessage().getChatId();

                if (text.equals("/start") || text.equals("/start@ITIS_FAQ_BOT"))
                    MESSAGE_HANDLER.sendMessage(chatId, "\uD83E\uDD16 FAQ-бот приёмной комиссии. Помогаю абитуриентам поступить!");
                else if (text.equals("/help") || text.equals("/help@ITIS_FAQ_BOT"))
                    MESSAGE_HANDLER.sendMessage(chatId, "\uD83D\uDCA1 Напиши /ask и задай вопрос");
                else if (text.equals("/ask") || text.equals("/ask@ITIS_FAQ_BOT")) {
                    MESSAGE_HANDLER.sendMessage(chatId, "\uD83D\uDEA8 Мы не обрабатываем пустые запросы");
                } else if (text.startsWith("/ask ") || text.startsWith("/ask@ITIS_FAQ_BOT ")) {
                    String question;
                    if (text.startsWith("/ask "))
                        question = text.replace("/ask ", "");
                    else
                        question = text.replace("/ask@ITIS_FAQ_BOT ", "");

                    String answer = FAQclient.ask(question);
                    answer = handleAnswer(answer, userId, chatId, question);
                    if (!answer.isEmpty()) {
                        // Отправляем ответ пользователю и получаем отправленное сообщение в случае успеха
                        Message answerMessage = MESSAGE_HANDLER.sendAnswer(chatId, answer);
                        MESSAGE_STORAGE.put(answerMessage.getMessageId(), new MessageStorage.QuestionInfo(userId, question));
                    } else {
//                        sendLowConfidenceAlert(chatId, question);
                        MESSAGE_HANDLER.sendMessage(chatId, "🚨 Не могу ответить на вопрос:\n\n" +
                                "❓ Вопрос:\n" + question + "\n\n" +
                                "\uD83D\uDCACПриемная комиссия: " + String.join(" ", Secrets.getAdmission()));
                    }
                }
            } else if (update.hasCallbackQuery()) {
                handleFeedback(update.getCallbackQuery());
            }
        }
    }

    private String handleAnswer(String answer, long userId, long chatId, String question) {
        if (answer.equals("LOW_CONFIDENCE")) {
            LogEntry log = new LogEntry(
                    userId,
                    chatId,
                    question,
                    answer,
                    0.0,
                    "Автоматически добавленный лог",
                    "LOW_CONFIDENCE"
            );
            LOGGER_BOT.addLog(log);
            return "";
        }
        answer = answer.replace("\"answer\":", "");
        answer = answer.replace("\"", "");
        answer = answer.replace("{", "");
        answer = answer.replace("}", "");
        return answer;
    }

    private void handleFeedback(CallbackQuery callbackQuery) {
        User pushedUser = callbackQuery.getFrom(); // тот, кто нажал на кнопку
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (!MESSAGE_STORAGE.isAsked(pushedUser.getId())) {
            // просто игнорим если фидбечит не тот, кто спрашивал текущий вопрос
            return;
        }

        String[] data = callbackQuery.getData().split(":");
        if (data.length != 2 || !data[0].equals("feedback"))
            return;

        String question = MESSAGE_STORAGE.get(messageId).getQuestion();
        MessageStorage.QuestionInfo info = MESSAGE_STORAGE.get(messageId);
        if (info == null)
            return; // сообщение не найдено


        String feedbackType = data[1];
        Message maybeInaccessibleMessage = (Message) callbackQuery.getMessage();

        String answer = maybeInaccessibleMessage.getText(); // Получаем оригинальный вопрос

        if (feedbackType.equals("no")) {
            LogEntry log = new LogEntry(
                    pushedUser.getId(),
                    chatId,
                    question,
                    answer, // Сохраняем полный текст вопроса
                    0.0,
                    "Пользователь отметил ответ как неполезный",
                    "BAD_FEEDBACK"
            );
            LOGGER_BOT.addLog(log);

            // Отправляем уведомление
            sendNegativeFeedbackAlert(chatId, question, messageId);
        }

        try {
            CLIENT.execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .replyMarkup(null)
                    .build());
        } catch (TelegramApiException e) {
            System.out.println("Ошибка во вреям удаления панели для фидбека!");
        }

        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(feedbackType.equals("yes") ? "Спасибо за отзыв! \uD83D\uDC4D" : "Мы учтем ваш отзыв и улучшим ответ! \uD83D\uDCDD")
                    .build());
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки подтверждения: " + e.getMessage());
        }
    }

    public void sendNegativeFeedbackAlert(long chatId, String question, Integer messageId) {
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
            System.err.println("Ошибка отправки алерта плохого фидбека: " + e.getMessage());
        }
        MESSAGE_STORAGE.remove(messageId); // и в конце, когда все операции с сообщением выполнены, мы его удаляем
    }

    public void sendLowConfidenceAlert(long chatId, String question) {
        String message = "🚨 Не могу ответить на вопрос:\n\n" +
                "❓ Вопрос:\n" + question + "\n\n" +
                "\uD83D\uDCACПриемная комиссия: " + String.join(" ", Secrets.getAdmission());

        SendMessage alert = SendMessage.builder()
                .chatId(chatId)
                .text(message)
                .build();

        try {
            CLIENT.execute(alert);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки алерта низкой уверенности: " + e.getMessage());
        }
    }
}