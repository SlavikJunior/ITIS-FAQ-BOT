package bot.handlers;

import bot.DevLoggerBot;
import bot.shared.LogEntry;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageHandler {
    private final TelegramClient CLIENT;

    public MessageHandler(TelegramClient client) {
        this.CLIENT = client;
    }

    public void sendAnswer(long chatId, String question,String answer) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(answer)
                .replyMarkup(createFeedbackButtons(question))
                .build();


        try {
            CLIENT.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки ответа: " + e.getMessage());
        }
    }

    private InlineKeyboardMarkup createFeedbackButtons(String question) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(rows);
        InlineKeyboardRow row = new InlineKeyboardRow();

        // в удобном формате передаём данные, разделяя двоеточием,
        // это пригодится при обработке нажатия на кнопку
        InlineKeyboardButton buttonY = InlineKeyboardButton.builder()
                .text("👍 Помогло")
                .callbackData("feedback:yes:" + question.hashCode())
                .build();
        InlineKeyboardButton buttonN = InlineKeyboardButton.builder()
                .text("👎 Не помогло")
                .callbackData("feedback:no:" + question.hashCode())
                .build();

        row.add(buttonY);
        row.add(buttonN);

        rows.add(row);
        inlineKeyboardMarkup.setKeyboard(rows);

        return  inlineKeyboardMarkup;
    }

}