package bot.shared;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageStorage {

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
}
