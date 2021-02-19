import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;

public class TestApp {
    public static void main(String[] args) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7000"))
                .build();

        Date startDate = new Date();
        for (int i = 0; i < 10_000; i++) {
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        }
        var finalTime = new Date().getTime() - startDate.getTime();
        System.out.println("end = " + finalTime);



    }
}
