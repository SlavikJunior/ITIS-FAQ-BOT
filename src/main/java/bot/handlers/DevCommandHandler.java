package bot.handlers;

import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import bot.shared.LogEntry;
import bot.DevLoggerBot;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class DevCommandHandler {
    private final TelegramClient CLIENT;
    private final DevLoggerBot BOT;
    private static final int MAX_INLINE_LOGS = 10; // Максимум логов в сообщении

    public DevCommandHandler(TelegramClient client, DevLoggerBot bot) {
        CLIENT = client;
        BOT = bot;
    }

    public void handle(Message message) {
        String text = message.getText().trim();
        long chatId = message.getChatId();

        if (text.equals("/start") || text.equals("/start@DEV_ITIS_FAQ_BOT"))
            sendMessage(chatId, "Этот бот нужен для отслеживания логов бота @ITIS_FAQ_BOT");
        else if (text.equals("/help") || text.equals("/help@DEV_ITIS_FAQ_BOT"))
            sendHelp(chatId);
        else if (text.equals("/logs") || text.equals("/logs@DEV_ITIS_FAQ_BOT"))
            sendLogsFile(chatId, BOT.getLogs(Integer.MAX_VALUE), "all_logs.txt");  // Все логи файлом
        else if (text.matches("/logs\\s+\\d+")) {
            int limit = Integer.parseInt(text.split("\\s+")[1]);
            if (limit <= MAX_INLINE_LOGS)
                sendLastLogs(chatId, limit); // Мало логов → сообщением
            else
                sendLogsFile(chatId, BOT.getLogs(limit), "last_" + limit + "_logs.txt");  // Много логов → файлом
        } else
            sendMessage(chatId, "Неопознанная команда!");
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
            sendMessage(chatId, "Логов нет.");
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
            🕒 *%s* | Уверенность: *%.2f*
            ❓ `%s`
            💬 `%s`
            ------------------------
            """, logType, log.getTimestamp(), log.getConfidence(),
                    log.getQuestion(), log.getAnswer()));
        }

        sendMarkdown(chatId, sb.toString());
    }


    private void sendHelp(long chatId) {
        String helpText = """
                📜 *Доступные команды*:
                `/logs` — все логи
                `/logs N` — последние N логов
                """;
        sendMarkdown(chatId, helpText);
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

    private void sendMessage(long chatId, String text) {
        SendMessage msg = new SendMessage(String.valueOf(chatId), text);
        try {
            CLIENT.execute(msg);
        } catch (TelegramApiException e) {
            System.out.println("Ошибка отправки сообщения из бота логера!");
        }
    }
}
