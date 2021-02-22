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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private final String url;
    private final ExecutorService responseEx = Executors.newFixedThreadPool(1);
    private final ExecutorService requestEx = Executors.newFixedThreadPool(1);
    private final HttpRequest licensesR;
    private final HttpRequest newLicenseR;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .executor(requestEx)
            .build();

    public Client(String address, int port) throws URISyntaxException {
        url = "http://" + address + ":" + port;
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

    public void dig(License license, int posX, int posY, int depth) {
        var digR = new DigRq(license.getId(), posX, posY, depth);
        httpClient.sendAsync(createDigRequest(digR), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        System.err.println("Success dig = " + response.body());
                        var treasures = JsonIterator.deserialize(response.body(), String[].class);
                        for (int i = 0; i < treasures.length; i++) {
                            getMyMoney(treasures[i]);
                        }
                        if (depth < 4) {
                            dig(license, posX, posY, depth + 1);
                        }
                    } else if (response.statusCode() == Const.HTTP_NOT_FOUND) {
                        if (depth < 4) {
                            dig(license, posX, posY, depth + 1);
                        }
                    } else {
                        System.err.println("Dig error = " + response.body());
                    }
                } ,responseEx);
    }

    public void getMyMoney(String treasureId) {
        httpClient.sendAsync(createCashRequest(treasureId), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        System.err.println("Money = " + response.body());
                    } else {
                        System.err.println("Money error = " + response.body());
                    }
                }, responseEx);
    }

    public void explore(Area area) {
        httpClient.sendAsync(createExploreRequest(area), HttpResponse.BodyHandlers.ofString())
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

    private HttpRequest createCashRequest(String treasureId) {
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(url + "/cash"))
                    .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString("\"" + treasureId + "\""))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("createCashRequest", e);
        }
    }

    private HttpRequest createDigRequest(DigRq digRq) {
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(url + "/dig"))
                    .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(digRq)))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("createDigRequest", e);
        }
    }

    private HttpRequest createExploreRequest(Area area) {
        try {
            return HttpRequest.newBuilder()
                    .uri(new URI(url + "/explore"))
                    .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(area)))
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("createExploreRequest", e);
        }
    }
}
