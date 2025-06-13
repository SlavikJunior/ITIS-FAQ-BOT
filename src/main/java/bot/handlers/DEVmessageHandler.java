package bot.handlers;

import bot.DevLoggerBot;
import bot.Secrets;
import bot.shared.AuthUtils;
import bot.shared.LogEntry;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Класс представляет собой интерфейс текстового взаимодействия @DEV_ITIS_FAQ_BOT
 * @author github.com/SlavikJunior, github.com/tensaid7
 * @version 1.0.1
 * @since 1.0.1
 **/

public class DEVmessageHandler {
    private final TelegramClient CLIENT;
    private final DevLoggerBot LOGGER_BOT;
    private static final int MAX_INLINE_LOGS = 10;

    public DEVmessageHandler(TelegramClient client, DevLoggerBot bot) {
        CLIENT = client;
        LOGGER_BOT = bot;
    }

    public void handle(Message message) {
        User user = message.getFrom();
        long chatId = message.getChatId();

        if (!AuthUtils.isDeveloper(user.getId())) {
            try {
                sendMessage(chatId, "🚫 Доступ только для разработчиков!");
            } catch (TelegramApiException e) {
                System.out.println("Ошибка отправки сообщения о запрете доступа!");
            }
            return;
        }

        String text = message.getText().trim();

        try {
            if (text.equals("/start") || text.equals("/start@DEV_ITIS_FAQ_BOT")) {
                handleStartCommand(chatId);
            } else if (text.equals("/help") || text.equals("/help@DEV_ITIS_FAQ_BOT")) {
                handleHelpCommand(chatId);
            } else if (text.equals("/admin") || text.equals("/admin@DEV_ITIS_FAQ_BOT")) {
                handleAdminCommand(chatId);
            } else if (text.equals("/logs") || text.equals("/logs@DEV_ITIS_FAQ_BOT")) {
                handleAllLogsCommand(chatId);
            } else if (text.matches("/logs\\s+\\d+")) {
                handleLimitedLogsCommand(chatId, text);
            } else if (text.equals("Конец жучьему криминалу") && message.getReplyToMessage() != null) {
                handleClearCommand(message);
            } else if (message.getReplyToMessage() != null) {
                handleReplyToMessage(message);
            } else {
                sendMessage(chatId, "Неопознанная команда! ⚠");
            }
        } catch (TelegramApiException e) {
            System.out.println("Ошибка обработки текстовой команды!");
        }
    }

    private void handleStartCommand(long chatId) throws TelegramApiException {
        sendMessage(chatId, "Этот бот нужен для отслеживания логов бота @ITIS_FAQ_BOT \uD83C\uDF93");
    }

    private void handleHelpCommand(long chatId) throws TelegramApiException {
        String helpText = """
                📜 Доступные команды:
                /start - начать использовать админского бота
                /help - получить это сообщение
                /admin - админская панель управления
                /logs — все логи
                /logs N — последние N логов
                """;
        sendMessage(chatId, helpText);
    }

    private void handleAdminCommand(long chatId) throws TelegramApiException {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("🛠️ Панель управления")
                .replyMarkup(createAdminKeyboard())
                .build();
        execute(message);
    }

    private void handleAllLogsCommand(long chatId) throws TelegramApiException {
        sendLogsFile(chatId, LOGGER_BOT.getLogs(Integer.MAX_VALUE), "all_logs.txt");
    }

    private void handleLimitedLogsCommand(long chatId, String text) throws TelegramApiException {
        int limit = Integer.parseInt(text.split("\\s+")[1]);
        if (limit <= MAX_INLINE_LOGS) {
            sendLastLogs(chatId, limit);
        } else {
            sendLogsFile(chatId, LOGGER_BOT.getLogs(limit), "last_" + limit + "_logs.txt");
        }
    }

    private void handleClearCommand(Message message) throws TelegramApiException {
        Secrets.clearAlarmUsers();
        sendMessage(message.getChatId(), "\uD83D\uDCCC Вот и закончился криминал");
    }

    private void handleReplyToMessage(Message message) throws TelegramApiException {
        Message repliedTo = message.getReplyToMessage();
        String requestText = repliedTo.getText();
        String userInput = message.getText().trim();
        long chatId = message.getChatId();

        if (requestText.contains("добавления")) {
            Secrets.handleAddRequest(chatId, userInput);
        } else if (requestText.contains("удаления")) {
            Secrets.handleRemoveRequest(chatId, userInput);
        } else if (requestText.contains("Введите ID жука и через пробел сообщение")) {
            Secrets.handleChangeRequest(chatId, message);
        } else if (requestText.contains("Введите ID жука, для которого нужно")) {
            Secrets.handleResetRequest(chatId, message);
        } else {
            sendMessage(chatId, "❌ Неверный формат ID. Введите только цифры");
        }

        deleteMessage(chatId, repliedTo.getMessageId());
    }

    private void sendLastLogs(long chatId, int limit) throws TelegramApiException {
        List<LogEntry> logs = LOGGER_BOT.getLogs(limit);
        if (logs.isEmpty()) {
            sendMessage(chatId, "⚠ Нет логов для экспорта");
            return;
        }

        StringBuilder sb = new StringBuilder("📜 *Последние " + limit + " логов:*\n\n");
        for (LogEntry log : logs) {
            String logType = switch (log.getType()) {
                case "BAD_FEEDBACK" -> "👎 *ПЛОХОЙ ОТЗЫВ*";
                case "LOW_CONFIDENCE" -> "⚠️ *НИЗКАЯ УВЕРЕННОСТЬ*";
                default -> "";
            };

            sb.append(String.format("""
                    %s
                    👤 *%s*
                    🕒 *%s* | Уверенность: *%.2f*
                    ❓ `%s`
                    💬 `%s`
                    ------------------------
                    """, logType, log.getUserId(), log.getTimestamp(), log.getConfidence(),
                    log.getQuestion(), log.getAnswer()));
        }

        sendMarkdownMessage(chatId, sb.toString());
    }

    private void sendLogsFile(long chatId, List<LogEntry> logs, String fileName) throws TelegramApiException {
        File file = null;
        try {
            if (logs == null || logs.isEmpty()) {
                sendMessage(chatId, "⚠️ Нет логов для экспорта");
                return;
            }

            File tempDir = new File(System.getProperty("java.io.tmpdir"), "telegram_logs");
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                throw new IOException("Не удалось создать директорию для логов");
            }

            file = new File(tempDir, fileName);

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {

                for (LogEntry log : logs) {
                    writer.write(String.format("[%s] Confidence: %.2f\nQ: %s\nA: %s\n\n",
                            log.getTimestamp(),
                            log.getConfidence(),
                            log.getQuestion(),
                            log.getAnswer()));
                }
                writer.flush();
            }

            if (file.length() == 0) {
                throw new IOException("Файл логов пуст");
            }

            sendDocument(SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(file, fileName))
                    .build());

        } catch (IOException e) {
            sendMessage(chatId, "📛 Ошибка создания файла логов: " + e.getMessage());
        } finally {
            if (file != null && !file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private InlineKeyboardMarkup createAdminKeyboard() {
        InlineKeyboardButton addButton = InlineKeyboardButton.builder()
                .text("\uD83E\uDEB5 Добавить")
                .callbackData("admin_add")
                .build();

        InlineKeyboardButton removeButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDD04 Удалить")
                .callbackData("admin_remove")
                .build();

        InlineKeyboardButton listButton = InlineKeyboardButton.builder()
                .text("📋 Список")
                .callbackData("admin_list")
                .build();

        InlineKeyboardButton clearButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDEA8 Очистить")
                .callbackData("admin_clear")
                .build();

        InlineKeyboardButton changeButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDCA1 Изменить ответ")
                .callbackData("admin_change")
                .build();

        InlineKeyboardButton resetButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDD04 Сбросить ответ")
                .callbackData("admin_reset")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(addButton, removeButton))
                .keyboardRow(new InlineKeyboardRow(listButton))
                .keyboardRow(new InlineKeyboardRow(changeButton, resetButton))
                .keyboardRow(new InlineKeyboardRow(clearButton))
                .build();
    }

    private void sendMarkdownMessage(long chatId, String text) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("Markdown")
                .build());
    }

    private void sendMessage(long chatId, String text) throws TelegramApiException {
        execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build());
    }

    private void sendDocument(SendDocument document) throws TelegramApiException {
        CLIENT.execute(document);
    }

    private void deleteMessage(long chatId, int messageId) throws TelegramApiException {
        CLIENT.execute(DeleteMessage.builder()
                .chatId(chatId)
                .messageId(messageId)
                .build());
    }

    // экзекьютор для общих методов
    private void execute(BotApiMethod<?> method) throws TelegramApiException{
        CLIENT.execute(method);
    }
}