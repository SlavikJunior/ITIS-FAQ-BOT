package bot.shared;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Класс, взаимодействующий с FastAPI приложением.
 * Посылает запрос пользователя и получает ответ модели.
 * @author github.com/SlavikJunior
 * @version 1.0.0
 * @since 1.0.0
 **/

public class FAQclient {

    private static final String URL_ADDRESS = "http://localhost:8000/ask";

    public static String ask(String question) {
        String jsonInputString = "{\"question\": \"" + question + "\"}";
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(URL_ADDRESS).openConnection();
        } catch (MalformedURLException e) {
            System.out.println("Ошибка конекшена!");
        } catch (IOException e) {
            System.out.println("Ошибка ввода-вывода!");
        }
        try {
            connection.setRequestMethod("POST");
        } catch (ProtocolException e) {
            System.out.println("Ошибка протокола!");
        }
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);


        try(OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        } catch (IOException e) {
            System.out.println("Ошибка ввода-вывода!");
        }

        StringBuilder response = new StringBuilder();
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода-вывода!");
        }

        return handleResponse(response.toString());
    }

    private static String handleResponse(String response) {
        if (response.contains("Извините, не нашёл ответа. Обратитесь к сотрудникам ИТИС с вопросом"))
            return "LOW_CONFIDENCE";
        return response;
    }
}
