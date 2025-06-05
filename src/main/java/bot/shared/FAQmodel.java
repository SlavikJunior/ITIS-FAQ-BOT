package bot.shared;

public class FAQmodel {
    // сюда потом загрузим модель
    private Object model;

    public FAQmodel(String modelPath) {
        loadModel(modelPath);
    }

    private void loadModel(String path) {
        // реализуем здесь потом бота
        try {
            // возмножно: this.model = FastText.loadModel(new File(path));
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки модели", e);
        }
    }

    public String getAnswer(String question) {
        // используем модель для генерации текста
        return "Ответ модели на: " + question;
    }

    public double getConfidence(String question) {
        // и вернуть уверенность ответа
        return 0.8;
    }
}