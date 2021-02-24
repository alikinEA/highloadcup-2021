package app.client;

import app.Application;
import app.client.models.*;
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
    private final ExecutorService responseEx = Executors.newFixedThreadPool(2);
    private final ExecutorService requestEx = Executors.newFixedThreadPool(2);
    private final HttpRequest newLicenseR;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .executor(requestEx)
            .build();

    public Client(String address, int port) throws URISyntaxException {
        url = "http://" + address + ":" + port;
        newLicenseR = HttpRequest.newBuilder()
                .uri(new URI(url + "/licenses"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("[]"))
                .build();
    }

    public HttpResponse<String> getNewLicense() throws IOException, InterruptedException, URISyntaxException {
        var cash = Repository.pollMoney();
        if (cash != null) {
            var response = httpClient.send(createPaidLicenseRequest(cash), HttpResponse.BodyHandlers.ofString());
            logger.error("Paid license purchase = " + response + Repository.getActionsInfo());
            return response;
        } else {
            //logger.error("No money = " + Repository.getActionsInfo());
            return httpClient.send(newLicenseR, HttpResponse.BodyHandlers.ofString());
        }
    }

    private HttpRequest createPaidLicenseRequest(Integer cash) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url + "/licenses"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("[" + cash + "]"))
                .build();
    }

    public void dig(DigFull fullDig) {
        httpClient.sendAsync(createDigRequest(fullDig.getDigRq()), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK || response.statusCode() == Const.HTTP_NOT_FOUND) {
                        var license = fullDig.getLicense();
                        var digRq = fullDig.getDigRq();
                        var amount = fullDig.getAmount();
                        var currentAmount = fullDig.getCurrentAmount();

                        license.setDigAllowed(license.getDigAllowed() - 1);
                        digRq.setDepth(digRq.getDepth() + 1);

                        if (response.statusCode() == Const.HTTP_OK) {
                            currentAmount.incrementAndGet();
                            Repository.incDigSuccess();
                            var treasures = JsonIterator.deserialize(response.body(), String[].class);
                            for (int i = 0; i < treasures.length; i++) {
                                getMyMoney(treasures[i]);
                            }
                        } else {
                            Repository.incDigMiss();
                        }
                        if (digRq.getDepth() == Application.GRABTIEFE && currentAmount.get() < amount) {
                            Repository.incTreasureNotFound();
                            //logger.error("Dug 10 time = " + fullDig + Repository.getActionsInfo());
                        }

                        if (digRq.getDepth() < Application.GRABTIEFE && currentAmount.get() < amount) {
                            if (license.getDigAllowed() > 0) {
                                dig(fullDig);
                            } else {
                                Repository.addDugFull(fullDig);
                            }
                        } else {
                            if (license.getDigAllowed() > 0) {
                                Repository.putLicense(license);
                            }
                        }
                    } else {
                        Repository.incDigError();
                        logger.error("Dig error = " + response.body() + fullDig);
                    }
                } ,responseEx);
    }

    public void getMyMoney(String treasureId) {
        getMyMoney(createCashRequest(treasureId));
    }

    public void getMyMoney(HttpRequest httpRequest) {
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        var cash = JsonIterator.deserialize(response.body(), int[].class);
                        for (int i : cash) {
                            Repository.addMoney(i);
                        }
                        if (Repository.incMoneySuccess() % 100 == 0) {
                            //logger.error("Money = " + Repository.getActionsInfo());
                        }
                    } else if (response.statusCode() == Const.HTTP_SERVICE_UNAVAILABLE) {
                        Repository.incMoneyError();
                        Repository.addMoneyRetry(response.request());
                    } else {
                        logger.error("Money error = " + response.body());
                    }
                }, responseEx);
    }

    public void explore(Area area) {
        explore(new ExploreFull(createExploreRequest(area), area));
    }

    public void explore(ExploreFull exploreFull) {
        httpClient.sendAsync(exploreFull.getHttpRequest(), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        var area = exploreFull.getArea();
                        Repository.incExplorerSuccess();
                        var explored = JsonIterator.deserialize(response.body(), Explored.class);
                        if (area.getSizeY() > 1 && explored.getAmount() > area.getSizeX() * area.getSizeY()) {
                            Repository.incRichArea();
                            for (int i = 0; i < area.getSizeY(); i++) {
                                explore(new Area(area.getPosX(), area.getPosY() + i, 1, 1));
                            }
                        } else if (area.getSizeY() > 1 && explored.getAmount() > 0 && Repository.exploredAreas1.size() < 100) {
                            for (int i = 0; i < area.getSizeY(); i++) {
                                explore(new Area(area.getPosX(), area.getPosY() + i, 1, 1));
                            }
                        } else if (area.getSizeY() == 1 && explored.getAmount() > 0) {
                            Repository.addExplored(explored);
                        }
                    } else if (response.statusCode() == Const.RATE_LIMIT) {
                        exploreFull.setHttpRequest(response.request());
                        Repository.exploreRetry(exploreFull);
                        //logger.error("explore error = " + response.body());
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
