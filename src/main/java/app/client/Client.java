package app.client;

import app.client.models.Area;
import app.client.models.DigRq;
import app.client.models.Explored;
import app.client.models.License;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class Client {
    private static final int REQUEST_QUEUE_SIZE = 10_000;
    private final ExecutorService responseEx = Executors.newFixedThreadPool(1);
    private final ExecutorService requestEx = new ThreadPoolExecutor(1, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(REQUEST_QUEUE_SIZE));
    private final String url;

    private final HttpRequest healCheckR;
    private final HttpRequest licensesR;
    private final HttpRequest newLicenseR;

    private final HttpClient httpClientMultiThread = HttpClient.newBuilder()
            .executor(requestEx)
            .build();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .build();

    public Client(String address, int port) throws URISyntaxException {
        url = "http://" + address + ":" + port;
        healCheckR = HttpRequest.newBuilder()
                .uri(new URI(url + "/heal-check"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .GET()
                .build();
        licensesR = HttpRequest.newBuilder()
                .uri(new URI(url + "/licenses"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .GET()
                .build();
        newLicenseR = HttpRequest.newBuilder()
                .uri(new URI(url + "/licenses"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("[]"))
                .build();
    }

    public HttpResponse<String> getLicenses() throws IOException, InterruptedException {
       return httpClient.send(licensesR, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getNewLicense() throws IOException, InterruptedException {
        return httpClient.send(newLicenseR, HttpResponse.BodyHandlers.ofString());
    }

    public void dig(License license, int posX, int posY, int depth) throws URISyntaxException, IOException, InterruptedException {
        var digR = new DigRq(license.getId(), posX, posY, depth);
        var response = httpClient.send(createDigRequest(digR), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == Const.HTTP_OK) {
            System.err.println("Success dig = " + response.body());
            var treasures = JsonIterator.deserialize(response.body(), String[].class);
            for (int i = 0; i < treasures.length; i++) {
                try {
                    getMyMoney(treasures[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Dig error = " + response.body());
        }
    }

    public void getMyMoney(String treasureId) throws URISyntaxException, IOException, InterruptedException {
        var response = httpClient.send(createCashRequest(treasureId), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == Const.HTTP_OK) {
            System.err.println("Money = " + response.body());
        } else {
            System.err.println("Money error = " + response.body());
        }
    }

    private HttpRequest createCashRequest(String treasureId) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url + "/cash"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("\"" + treasureId + "\""))
                .build();
    }

    private HttpRequest createDigRequest(DigRq digRq) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url + "/dig"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(digRq)))
                .build();
    }

    public CompletableFuture<Void> explore(Area area) throws URISyntaxException {
        return httpClientMultiThread.sendAsync(createExploreRequest(area), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        var explored = JsonIterator.deserialize(response.body(), Explored.class);
                        if (explored.getAmount() > 0) {
                            Repository.putExplored(explored);
                            System.err.println("Put explored = " + explored);
                        }
                    } else {
                        System.err.println("explore error = " + response.body());
                    }
                }, responseEx);
    }

    private HttpRequest createExploreRequest(Area area) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url + "/explore"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(area)))
                .build();
    }
}
