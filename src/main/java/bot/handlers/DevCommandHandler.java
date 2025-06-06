package bot.handlers;

import bot.DevLoggerBot;
import bot.Secrets;
import bot.shared.LogEntry;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class DevCommandHandler {
    private static TelegramClient CLIENT;
    private final DevLoggerBot BOT;
    private static final int MAX_INLINE_LOGS = 10;// Максимум логов в сообщении


    public DevCommandHandler(TelegramClient client, DevLoggerBot loggerBot) {
        CLIENT = client;
        this.BOT = loggerBot;
    }

    public void handle(Message message) {
        String text = message.getText().trim();
        long chatId = message.getChatId();

        if (text.equals("/start") || text.equals("/start@DEV_ITIS_FAQ_BOT"))
            sendMessage(chatId, "Этот бот нужен для отслеживания логов бота @ITIS_FAQ_BOT \uD83C\uDF93");
        else if (text.equals("/admin") || text.equals("/admin@DEV_ITIS_FAQ_BOT"))
            sendAdminPanel(message.getChatId());
        else if (text.equals("/help") || text.equals("/help@DEV_ITIS_FAQ_BOT")) {
            sendMessage(chatId, """
                    📜 Доступные команды:
                    /start - начать использовать админского бота
                    /help - получить это сообщение
                    /admin - админская панель управления жучим криминалом
                    /logs — все логи
                    /logs N — последние N логов
                    """);
        } else if (text.equals("/logs") || text.equals("/logs@DEV_ITIS_FAQ_BOT"))
            sendLogsFile(chatId, BOT.getLogs(Integer.MAX_VALUE), "all_logs.txt");  // Все логи файлом
        else if (text.matches("/logs\\s+\\d+")) {
            int limit = Integer.parseInt(text.split("\\s+")[1]);
            if (limit <= MAX_INLINE_LOGS)
                sendLastLogs(chatId, limit); // Мало логов → сообщением
            else
                sendLogsFile(chatId, BOT.getLogs(limit), "last_" + limit + "_logs.txt");  // Много логов → файлом
        } else if (message.getReplyToMessage() != null && message.getReplyToMessage().hasText()) {
            Message repliedTo = message.getReplyToMessage();
            if (repliedTo.getText().equals("❓ Ты сделаешь это?\nТогда введи: <Конец жучьему криминалу> в ответ на это сообщение \uD83D\uDEA8") &&
                    message.hasText() && message.getText().equals("Конец жучьему криминалу")) {
                Secrets.clearAlarmUsersIds();
                try {
                    execute(SendMessage.builder()
                            .text("\uD83D\uDCCC Вот и закончился криминал")
                            .chatId(chatId)
                            .build());
                } catch (TelegramApiException e) {
                    System.out.println("Ошибка отправки уведомления конца криминала!");
                }
            }
        } else
            sendMessage(chatId, "Неопознанная команда! ⚠");
    }

    private void sendLogsFile(long chatId, List<LogEntry> logs, String fileName) {
        File file = null;
        try {
            // 1. Проверяем есть ли логи для отправки
            if (logs == null || logs.isEmpty()) {
                sendMessage(chatId, "⚠️ Нет логов для экспорта");
                return;
            }

            // 2. Создаем временный файл в специальной директории
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "telegram_logs");
            if (!tempDir.exists() && !tempDir.mkdirs()) {
                throw new IOException("Не удалось создать директорию для логов");
            }

            file = new File(tempDir, fileName);

            // 3. Записываем логи с кодировкой UTF-8
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

            // 4. Проверяем что файл создан и не пустой
            if (file.length() == 0) {
                throw new IOException("Файл логов пуст");
            }

            // 5. Отправляем файл с обработкой таймаута
            SendDocument doc = SendDocument.builder()
                    .chatId(chatId)
                    .document(new InputFile(file, fileName))
                    .build();

            CLIENT.execute(doc);

        } catch (IOException e) {
            sendMessage(chatId, "📛 Ошибка создания файла логов: " + e.getMessage());
        } catch (TelegramApiException e) {
            sendMessage(chatId, "📛 Ошибка отправки логов в Telegram");
        } finally {
            // 6. Удаляем временный файл в любом случае
            if (file != null && !file.delete()) {
                file.deleteOnExit();
            }
        }
    }

    private void sendLastLogs(long chatId, int limit) {
        List<LogEntry> logs = BOT.getLogs(limit);
        if (logs.isEmpty()) {
            sendMessage(chatId, "⚠ Нет логов для экспорта");
            return;
        }

        StringBuilder sb = new StringBuilder("📜 *Последние " + limit + " логов:*\n\n");
        for (LogEntry log : logs) {
            String logType = "";
            if (log.getType().equals("BAD_FEEDBACK")) {
                logType = "👎 *ПЛОХОЙ ОТЗЫВ*";
            } else if (log.getType().equals("LOW_CONFIDENCE")) {
                logType = "⚠️ *НИЗКАЯ УВЕРЕННОСТЬ*";
            }

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

        sendMarkdown(chatId, sb.toString());
    }

    private void sendMarkdown(long chatId, String text) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        msg.enableMarkdown(true);
        try {
            CLIENT.execute(msg);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки сообщения из бота логера!");
        }
    }

    private void sendAdminPanel(long chatId) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("🛠️ Панель управления жуками")
                .replyMarkup(createAdminKeyboard())
                .build();

        try {
            CLIENT.execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки админ-панели: " + e.getMessage());
        }
    }

    private InlineKeyboardMarkup createAdminKeyboard() {
        InlineKeyboardButton addButton = InlineKeyboardButton.builder()
                .text("\uD83E\uDEB5 Добавить жука")
                .callbackData("admin_add")
                .build();

        InlineKeyboardButton removeButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDD04 Удалить жука")
                .callbackData("admin_remove")
                .build();

        InlineKeyboardButton listButton = InlineKeyboardButton.builder()
                .text("📋 Список жуков")
                .callbackData("admin_list")
                .build();

        InlineKeyboardButton clearButton = InlineKeyboardButton.builder()
                .text("\uD83D\uDEA8 Очистить список жуков")
                .callbackData("admin_clear")
                .build();

        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(addButton, removeButton))
                .keyboardRow(new InlineKeyboardRow(listButton))
                .keyboardRow(new InlineKeyboardRow(clearButton))
                .build();
    }

    public void handleAdminCallback(CallbackQuery callbackQuery) {
        long chatId = callbackQuery.getMessage().getChatId();
        String action = callbackQuery.getData();

        try {
            switch (action) {
                case "admin_add":
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text("\uD83E\uDEB5 Введите ID жука для добавления:")
                            .replyMarkup(createCancelKeyboard())
                            .build());
                    break;

                case "admin_remove":
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text("\uD83D\uDD04 Введите ID жука для удаления:")
                            .replyMarkup(createCancelKeyboard())
                            .build());
                    break;

                case "admin_list":
                    Set<String> users = Secrets.getAlarmUserIds();
                    String response = users.isEmpty()
                            ? "📋 Список жуков пуст"
                            : "📋 Список жуков:\n" + String.join("\n", users);
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(response)
                            .build());
                    break;

                case "admin_clear":
                    String answer = "Ты сделаешь это? ❓\nТогда введи: <Конец жучьему криминалу> в ответ на это сообщение \uD83D\uDEA8";

                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(answer)
                            .build());
                    break;

                case "admin_cancel":
                    execute(DeleteMessage.builder()
                            .chatId(chatId)
                            .messageId(callbackQuery.getMessage().getMessageId())
                            .build());
                    return;
            }

            // Удаляем исходную клавиатуру
            execute(EditMessageReplyMarkup.builder()
                    .chatId(chatId)
                    .messageId(callbackQuery.getMessage().getMessageId())
                    .replyMarkup(null)
                    .build());

        } catch (TelegramApiException e) {
            System.err.println("Ошибка обработки callback: " + e.getMessage());
        }
    }

    public void handleTextAfterCallback(Message message) {
        Message repliedTo = message.getReplyToMessage();
        if (repliedTo != null && repliedTo.hasText()) {
            String requestText = repliedTo.getText();
            String userInput = message.getText().trim();
            long chatId = message.getChatId();

            try {
                if (requestText.contains("добавления")) {
                    Secrets.handleAddRequest(chatId, userInput);
                } else if (requestText.contains("удаления"))
                    Secrets.handleRemoveRequest(chatId, userInput);
                else {
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text("❌ Неверный формат ID. Введите только цифры")
                            .build());
                }

                // Удаляем сообщение с запросом
                execute(DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(repliedTo.getMessageId())
                        .build());

            } catch (TelegramApiException e) {
                System.err.println("Ошибка обработки ответа: " + e.getMessage());
            }
        }
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

    private void sendMessage(long chatId, String text) {
        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    private void execute(SendMessage message) throws TelegramApiException {
        CLIENT.execute(message);
    }

    private void execute(EditMessageReplyMarkup editMarkup) throws TelegramApiException {
        CLIENT.execute(editMarkup);
    }

    private void execute(DeleteMessage deleteMessage) throws TelegramApiException {
        CLIENT.execute(deleteMessage);
    }

    public static void sendMessageStatic(long chatId, String text) {
        try {
            CLIENT.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки сообщения!");
        }
    }
}