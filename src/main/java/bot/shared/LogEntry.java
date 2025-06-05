package bot.shared;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry {

    private long userId;
    private long chatId;
    private String question;
    private String answer;
    private double confidence;
    private String timestamp;
    private String type; // "LOW_CONFIDENCE", "BAD_FEEDBACK"
    private String feedbackComment;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public long getUserId() {
        return userId;
    }

    public long getChatId() {
        return chatId;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getFeedbackComment() {
        return feedbackComment;
    }

    public String getType() {
        return type;
    }

    public LogEntry(long userId, long chatId, String question, String answer, double confidence, String feedbackComment, String type) {
        this.userId = userId;
        this.chatId = chatId;
        this.question = question;
        this.answer = answer;
        this.confidence = confidence;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
        this.feedbackComment = feedbackComment;
        this.type = type;
    }
}