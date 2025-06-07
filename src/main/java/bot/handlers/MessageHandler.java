package bot.handlers;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.ArrayList;
import java.util.List;

public class MessageHandler {
    private final TelegramClient CLIENT;

    public MessageHandler(TelegramClient client) {
        this.CLIENT = client;
    }

    public void sendMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .text(text)
                .chatId(chatId)
                .build();
        try {
            CLIENT.execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки сообщения");
        }
    }

    public Message sendAnswer(long chatId, String answer) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .replyMarkup(createFeedbackButtons())
                .build();
        try {
            return CLIENT.execute(message);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки ответа: " + e.getMessage());
        }
        return null;
    }

    private InlineKeyboardMarkup createFeedbackButtons() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(rows);
        InlineKeyboardRow row = new InlineKeyboardRow();

        // в удобном формате передаём данные, разделяя двоеточием,
        // это пригодится при обработке нажатия на кнопку
        InlineKeyboardButton buttonY = InlineKeyboardButton.builder()
                .text("👍 Помогло")
                .callbackData("feedback:yes")
                .build();
        InlineKeyboardButton buttonN = InlineKeyboardButton.builder()
                .text("👎 Не помогло")
                .callbackData("feedback:no")
                .build();

        row.add(buttonY);
        row.add(buttonN);

        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);

        return  inlineKeyboardMarkup;
    }

}