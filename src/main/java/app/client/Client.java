package app.client;

import app.client.models.DigRq;
import app.client.models.License;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    ExecutorService responseEx = Executors.newFixedThreadPool(1);
    ExecutorService requestEx = Executors.newFixedThreadPool(1);
    private final String url;

    private final HttpRequest healCheckR;
    private final HttpRequest licensesR;
    private final HttpRequest newLicenseR;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .executor(requestEx)
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

    public CompletableFuture<Void> dig(License license, int posX, int posY, int depth) throws URISyntaxException {
        var digR = new DigRq(license.getId(), posX, posY, depth);
        return httpClient.sendAsync(createDigRequest(digR), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        System.err.println(response.body());
                        var treasures = JsonIterator.deserialize(response.body(), String[].class);
                        for (int i = 0; i < treasures.length; i++) {
                            try {
                                getMyMoney(treasures[i]);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }, responseEx);

    }

    public CompletableFuture<Void> getMyMoney(String treasureId) throws URISyntaxException {
        return httpClient.sendAsync(createCashRequest(treasureId), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    System.err.println("Money = " + response.body());
                }, responseEx);

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
}
