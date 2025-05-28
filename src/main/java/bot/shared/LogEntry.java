package bot.shared;

import java.time.Instant;

public class LogEntry {

    private long userId;
    private long chatId;
    private String question;
    private String answer;
    private double confidence;
    private Instant timestamp;
    private String type; // "LOW_CONFIDENCE", "BAD_FEEDBACK"

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

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public LogEntry(long userId, long chatId, String question, String answer, double confidence, Instant timestamp, String type) {
        this.userId = userId;
        this.chatId = chatId;
        this.question = question;
        this.answer = answer;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.type = type;
    }
}