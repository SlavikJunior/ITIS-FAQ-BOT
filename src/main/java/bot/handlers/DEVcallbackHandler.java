package bot.handlers;

import bot.Secrets;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import java.util.Set;

/**
 * Класс представляет собой интерфейс callback взаимодействия @DEV_ITIS_FAQ_BOT
 *
 * @author github.com/SlavikJunior, github.com/tensaid7
 * @version 1.0.1
 * @since 1.0.1
 **/

public class DEVcallbackHandler {
    private final TelegramClient CLIENT;

    public DEVcallbackHandler(TelegramClient client) {
        CLIENT = client;
    }

    public void handle(CallbackQuery callbackQuery) {
        String action = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        int messageId = callbackQuery.getMessage().getMessageId();

        try {
            switch (action) {
                case "admin_add":
                    handleAddAction(chatId);
                    break;
                case "admin_remove":
                    handleRemoveAction(chatId);
                    break;
                case "admin_list":
                    handleListAction(chatId);
                    break;
                case "admin_clear":
                    handleClearAction(chatId);
                    break;
                case "admin_change":
                    handleChangeAction(chatId);
                    break;
                case "admin_reset":
                    handleResetAction(chatId);
                    break;
                case "admin_cancel":
                    handleCancelAction(chatId, messageId);
                    return;
                default:
                    try {
                        execute(SendMessage.builder()
                                .chatId(chatId)
                                .text("⚠ Неизвестное действие")
                                .build());
                    } catch (TelegramApiException e) {
                        System.out.println("Ошибка отправки сообщения!");
                    }
                    return;
            }

            removeReplyMarkup(chatId, messageId);
        } catch (TelegramApiException e) {
            handleCallbackError(chatId, e);
        }
    }

    private void handleAddAction(long chatId) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("\uD83E\uDEB5 Введите ID жука для добавления:")
                .replyMarkup(createCancelKeyboard())
                .build());
    }

    private void handleRemoveAction(long chatId) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDD04 Введите ID жука для удаления:")
                .replyMarkup(createCancelKeyboard())
                .build());
    }

    private void handleListAction(long chatId) throws TelegramApiException {
        Set<Secrets.AlarmUser> users = Secrets.getAlarmUsers();
        String response = users.isEmpty()
                ? "📋 Список жуков пуст"
                : "📋 Список жуков:\n" + String.join("\n", users.toString());

        execute(SendMessage.builder()
                .chatId(chatId)
                .text(response)
                .build());
    }

    private void handleClearAction(long chatId) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("❓ Ты сделаешь это?\nТогда введи: <Конец жучьему криминалу> в ответ на это сообщение \uD83D\uDEA8")
                .build());
    }

    private void handleChangeAction(long chatId) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDD04 Введите ID жука и через пробел сообщение, которое он должен увидеть:")
                .replyMarkup(createCancelKeyboard())
                .build());
    }

    private void handleResetAction(long chatId) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDD04 Введите ID жука, для которого нужно задать стандартный ответ:")
                .replyMarkup(createCancelKeyboard())
                .build());
    }

    private void handleCancelAction(long chatId, int messageId) throws TelegramApiException {
        execute(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    private void removeReplyMarkup(long chatId, int messageId) throws TelegramApiException {
        execute(EditMessageReplyMarkup.builder()
                .chatId(chatId)
                .messageId(messageId)
                .replyMarkup(null)
                .build());
    }

    private InlineKeyboardMarkup createCancelKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(
                        InlineKeyboardButton.builder()
                                .text("❌ Отмена")
                                .callbackData("admin_cancel")
                                .build()
                ))
                .build();
    }

    private void handleCallbackError(long chatId, TelegramApiException e) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("⚠ Ошибка обработки запроса. Попробуйте позже")
                    .build());
            System.err.println("Callback error: " + e.getMessage());
        } catch (Exception ex) {
            System.err.println("Double error: " + ex.getMessage());
        }
    }

    // экзекьютор для общих методов
    private void execute(BotApiMethod<?> method) throws TelegramApiException {
        CLIENT.execute(method);
    }
}