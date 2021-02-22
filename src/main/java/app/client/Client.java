package app.client;

import app.client.models.Area;
import app.client.models.DigRq;
import app.client.models.Explored;
import app.client.models.License;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

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

    public void dig(DigRq digRq) {
        httpClient.sendAsync(createDigRequest(digRq), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    var digRPS = Repository.digRPS.incrementAndGet();
                    if (response.statusCode() == Const.HTTP_OK) {
                        logger.error("digRPS = " + digRPS + " Success dig = " + response.body() + digRq);
                        var treasures = JsonIterator.deserialize(response.body(), String[].class);
                        for (int i = 0; i < treasures.length; i++) {
                            getMyMoney(treasures[i]);
                        }
                        if (digRq.getDepth() == 3) {
                            return;
                        }
                        digRq.setDepth(digRq.getDepth() + 1);
                        dig(digRq);
                    } else if (response.statusCode() == Const.HTTP_NOT_FOUND) {
                        logger.error("digRPS = " + digRPS + " Dig not found = " + response.body() + digRq);
                        if (digRq.getDepth() == 3) {
                            return;
                        }
                        digRq.setDepth(digRq.getDepth() + 1);
                        dig(digRq);
                    } else {
                        logger.error("digRPS = " + digRPS + " Dig error = " + response.body() + digRq);
                    }
                } ,responseEx);
    }

    public void getMyMoney(String treasureId) {
        httpClient.sendAsync(createCashRequest(treasureId), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        //System.err.println("Money = " + response.body());
                    } else {
                        logger.error("Money error = " + response.body());
                    }
                }, responseEx);
    }

    public void explore(Area area) {
        httpClient.sendAsync(createExploreRequest(area), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    logger.error("exploreRPS = " + Repository.explorerRPS.incrementAndGet());
                    if (response.statusCode() == Const.HTTP_OK) {
                        var explored = JsonIterator.deserialize(response.body(), Explored.class);
                        if (explored.getAmount() > 0) {
                            Repository.addExplored(explored);
                        }
                    } else {
                        logger.error("explore error = " + response.body());
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
