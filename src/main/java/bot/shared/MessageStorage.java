package bot.shared;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс отвечает за хранение привязки вопроса к отправителю и текста вопроса.
 * Этот функционал нужен для корректной обработки того, кто может оценить ответ
 *
 * @author github.com/SlavikJunior
 * @version 1.0.0
 * @since 1.0.0
 **/

public class MessageStorage {

    /**
     * Класс, описывающий, контекст вопросов во время,
     * когда модель не дала ответ
     *
     * @author github.com/tensaid7
     * @version 1.0.1
     * @since 1.0.0
     **/

    public static class PendingQuestion {
        public long userId; // id спрашивающего
        public String username; // его username для упоминания
        public long chatId; // чат, куда отправим ответ
    }

    private final Map<Integer, PendingQuestion> pendingQuestions = new ConcurrentHashMap<>();
    private final Map<Long, Integer> adminToMessageMap = new ConcurrentHashMap<>();
    private final Set<Integer> answeredMessages = new HashSet<>();

    /**
     * Класс, описывающий, какая информация будет храниться под конкретным id сообщения
     *
     * @author github.com/SlavikJunior
     * @version 1.0.1
     * @since 1.0.0
     **/

    public static class QuestionInfo {
        private Long userId;
        private String question;

        public QuestionInfo(long userId, String question) {
            this.userId = userId;
            this.question = question;
        }

        public Long getUserId() {
            return userId;
        }

        public String getQuestion() {
            return question;
        }
    }

    private final Map<Integer, QuestionInfo> MAP_OF_MESSAGE_ID_AND_QUESTION_INFO = new ConcurrentHashMap<>();
    private final int MAX_SIZE = 1000;

    public void put(Integer messageId, QuestionInfo questionInfo) {
        if (MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.size() >= MAX_SIZE)
            MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.clear();
        MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.put(messageId, questionInfo);
    }

    public QuestionInfo get(Integer messageId) {
        if (MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.containsKey(messageId))
            return MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.get(messageId);
        return new QuestionInfo(0l, "");
    }

    public void remove(Integer messageId) {
        MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.remove(messageId);
    }

    public boolean isAsked(Long userId) {
        for (Map.Entry<Integer, QuestionInfo> entry : MAP_OF_MESSAGE_ID_AND_QUESTION_INFO.entrySet()) {
            if (entry.getValue().getUserId().equals(userId))
                return true;
        }
        return false;
    }

    public void addPendingQuestion(int messageId, long userId, String username, long chatId) {
        if (pendingQuestions.size() >= MAX_SIZE)
            pendingQuestions.clear();

        PendingQuestion question = new PendingQuestion();
        question.userId = userId;
        question.username = username;
        question.chatId = chatId;
        pendingQuestions.put(messageId, question);
    }

    public void setAdminResponse(long adminId, Integer messageId) {
        if (messageId == null)
            adminToMessageMap.remove(adminId);
        else
            adminToMessageMap.put(adminId, messageId);
    }

    public PendingQuestion getPendingQuestion(int messageId) {
        return pendingQuestions.get(messageId);
    }

    public void clearAdminState(long adminId) {
        adminToMessageMap.remove(adminId);
    }

    public void removePendingQuestion(int messageId) {
        pendingQuestions.remove(messageId);
        if (answeredMessages.size() >= MAX_SIZE)
            answeredMessages.clear();
        answeredMessages.add(messageId);
    }

    public boolean isAdminResponding(long adminId) {
        return adminToMessageMap.containsKey(adminId);
    }

    public Integer getAdminMessageId(long adminId) {
        return adminToMessageMap.get(adminId);
    }
}
