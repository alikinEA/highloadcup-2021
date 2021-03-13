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
import java.util.concurrent.CompletableFuture;
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
    private HttpRequest createPaidLicenseRequest(Integer cash) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url + "/licenses"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString("[" + cash + "]"))
                .build();
    }

    private void handleDigResponse(HttpResponse<String> response, DigFull fullDig) {
        if (response.statusCode() == Const.HTTP_OK || response.statusCode() == Const.HTTP_NOT_FOUND) {
            Repository.rpsSuccess.incrementAndGet();
            var license = fullDig.getLicense();
            var digRq = fullDig.getDigRq();
            var amount = fullDig.getAmount();
            var currentAmount = fullDig.getCurrentAmount();

            license.setDigAllowed(license.getDigAllowed() - 1);
            digRq.setDepth(digRq.getDepth() + 1);

            if (response.statusCode() == Const.HTTP_OK) {
                fullDig.setCurrentAmount(fullDig.getCurrentAmount() + 1);
                Repository.incDigSuccess();
                //logger.error("Dig success = " + fullDig + Repository.getActionsInfo());
                var treasures = JsonIterator.deserialize(response.body(), String[].class);
                for (int i = 0; i < treasures.length; i++) {
                    getMyMoney(treasures[i]);
                }
            } else {
                Repository.incDigMiss();
            }
            if (digRq.getDepth() == Application.GRABTIEFE && fullDig.getCurrentAmount() < amount) {
                Repository.incTreasureNotFound();
                //logger.error("Dug 10 time = " + fullDig + Repository.getActionsInfo());
            }

            if (digRq.getDepth() < Application.GRABTIEFE && fullDig.getCurrentAmount() < amount) {
                if (license.getDigAllowed() > 0) {
                    digAsync(fullDig);
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
    }

    public void getMyMoney(String treasureId) {
        getMyMoney(createCashRequest(treasureId));
    }

    public void getMyMoney(HttpRequest httpRequest) {
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        Repository.rpsSuccess.incrementAndGet();
                        Repository.incMoneySuccess();
                        var cash = JsonIterator.deserialize(response.body(), int[].class);
                        for (int i : cash) {
                            Repository.addMoney(i);
                        }
                    } else {
                        Repository.incMoneyError();
                        Repository.addMoneyRetry(response.request());
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

    public CompletableFuture<HttpResponse<String>> exploreBlocking(HttpRequest request) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
    public CompletableFuture<HttpResponse<String>> exploreBlocking(Area area) {
        return exploreBlocking(createExploreRequest(area));
    }

    public void getNewPaidLicenseAsync(Integer cash) throws URISyntaxException, IOException, InterruptedException {
        httpClient.sendAsync(createPaidLicenseRequest(cash), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> {
                    Repository.licenseAttempt.incrementAndGet();
                    if (response.statusCode() != Const.HTTP_OK) {
                        Repository.incLicenseErrors();
                        Repository.addMoney(cash);
                    } else {
                        Repository.rpsSuccess.incrementAndGet();
                        var license = JsonIterator.deserialize(response.body(), License.class);
                        Repository.putLicenseNew(license);
                    }
                    }, responseEx);
    }

    public HttpResponse<String> getNewFreeLicense() throws IOException, InterruptedException {
        return httpClient.send(newLicenseR, HttpResponse.BodyHandlers.ofString());
    }

    public void digAsync(DigFull digFull) {
        httpClient.sendAsync(createDigRequest(digFull.getDigRq()), HttpResponse.BodyHandlers.ofString())
                .thenAcceptAsync(response -> handleDigResponse(response, digFull));
    }

    public Explored doExplore(Area area) {
        while (true) {
            try {
                var responseF = exploreBlocking(area);
                var response = responseF.get();
                if (response.statusCode() != Const.HTTP_OK) {
                    Thread.sleep(10);
                } else {
                    Repository.rpsSuccess.incrementAndGet();
                    Repository.incExplorerSuccess();
                    return JsonIterator.deserialize(response.body(), Explored.class);
                }
            } catch (Exception e) {
                try {
                    Thread.sleep(10);
                } catch (Exception e1) {
                }
            }
        }
    }


    public void explore63(Area area) {
        try {
            var response = httpClient.send(createExploreRequest(area), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == Const.HTTP_OK) {
                Repository.rpsSuccess.incrementAndGet();
                var explored = JsonIterator.deserialize(response.body(), Explored.class);
                if (explored.getAmount() > 2) {
                    Repository.explored63Done.incrementAndGet();
                    Repository.exploredAreas63.put(explored);
                } else {
                    Repository.skipped63.incrementAndGet();
                }
            } else {
                Repository.incExplorerError();
            }
        } catch (Exception e) {
            Repository.incExplorerError();
        }
    }
}
