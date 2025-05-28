package bot.handlers;

import bot.DevLoggerBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class MessageHandler {
    private final TelegramClient CLIENT;


    public MessageHandler(TelegramClient client) {
        this.CLIENT = client;
    }

    public void sendAnswer(long chatId, String answer) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .build();

        try {
            CLIENT.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки ответа: " + e.getMessage());
        }
    }
}