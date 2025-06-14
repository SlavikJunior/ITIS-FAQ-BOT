package bot.shared;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * Класс, взаимодействующий с FastAPI веб-сервером.
 * Посылает запрос пользователя и получает ответ модели.
 * @author github.com/tensaid7
 * @version 1.0.2
 * @since 1.0.0
 **/

public class FAQclient {

    private static final String URL_ADDRESS =
            System.getenv().getOrDefault("PYTHON_API_URL", "http://localhost:8000") + "/ask";

    public static String ask(String question) {
        String jsonInputString = "{\"question\": \"" + question + "\"}";

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(URL_ADDRESS).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonInputString.getBytes("utf-8"));
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                return handleResponse(br.lines().collect(Collectors.joining()));
            }
        } catch (Exception e) {
            System.err.println("Ошибка при запросе к FastAPI: " + e.getMessage());
            return "ERROR";
        }
    }

    private static String handleResponse(String response) {
        if (response.contains("Извините, не нашёл ответа. Обратитесь к сотрудникам ИТИС с вопросом"))
            return "LOW_CONFIDENCE";
        return response;
    }
}
