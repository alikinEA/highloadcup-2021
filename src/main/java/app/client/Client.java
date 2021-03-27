package app.client;

import app.Application;
import app.client.models.*;
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
    private final URI licensesURI;
    private final URI cashURI;
    private final URI digURI;
    private final URI exploreURI;
    private final ExecutorService responseEx = Executors.newFixedThreadPool(1);
    private final ExecutorService requestEx = Executors.newFixedThreadPool(1);
    private final HttpRequest newLicenseR;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .executor(requestEx)
            .build();

    public Client(String address, int port) throws URISyntaxException {
        url = "http://" + address + ":" + port;
        licensesURI = new URI(url + "/licenses");
        cashURI = new URI(url + "/cash");
        digURI = new URI(url + "/dig");
        exploreURI = new URI(url + "/explore");
        newLicenseR = HttpRequest.newBuilder()
                .uri(licensesURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofByteArray(Const.EMPTY_ARRAY))
                .build();
    }
    private HttpRequest createPaidLicenseRequest(Integer cash) {
        byte[] body = wrap(cash.toString(), Const.BRACKET_O, Const.BRACKET_C);
        return HttpRequest.newBuilder()
                .uri(licensesURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private HttpRequest createCashRequest(String treasureId) {
        byte[] body = wrap(treasureId, Const.QUOTE, Const.QUOTE);
        return HttpRequest.newBuilder()
                .uri(cashURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    private byte[] wrap(String string, byte symbol_o, byte symbol_c) {
        byte[] trIdBytes = string.getBytes();
        byte[] body = new byte[trIdBytes.length + 2];
        body[0] = symbol_o;

        System.arraycopy(trIdBytes, 0, body, 1, trIdBytes.length);
        body[body.length - 1] = symbol_c;
        return body;
    }

    private HttpRequest createDigRequest(DigRq digRq) {
        return HttpRequest.newBuilder()
                .uri(digURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(digRq)))
                .build();
    }

    private HttpRequest createExploreRequest(Area area) {
        return HttpRequest.newBuilder()
                .uri(exploreURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(area)))
                .build();
    }

    private void handleDigResponse(HttpResponse<byte[]> response, DigFull fullDig) {
        if (response.statusCode() == Const.HTTP_OK || response.statusCode() == Const.HTTP_NOT_FOUND) {
            Repository.rpsSuccess.incrementAndGet();
            var license = fullDig.getLicense();
            var digRq = fullDig.getDigRq();
            var amount = fullDig.getAmount();

            license.setDigAllowed(license.getDigAllowed() - 1);
            digRq.setDepth(digRq.getDepth() + 1);

            if (response.statusCode() == Const.HTTP_OK) {
                fullDig.setCurrentAmount(fullDig.getCurrentAmount() + 1);
                Repository.incDigSuccess();
                if (digRq.getDepth() > 3) {
                    var treasures = JsonIterator.deserialize(response.body(), String[].class);
                    for (int i = 0; i < treasures.length; i++) {
                        getMyMoney(treasures[i]);
                    }
                } else {
                    Repository.skipTreasure.incrementAndGet();
                }
            } else {
                Repository.incDigMiss();
            }
            if (digRq.getDepth() == Application.GRABTIEFE && fullDig.getCurrentAmount() < amount) {
                Repository.incTreasureNotFound();
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
        }
    }

    public void getMyMoney(String treasureId) {
        getMyMoney(createCashRequest(treasureId));
    }

    public void getMyMoney(HttpRequest httpRequest) {
        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                .thenAcceptAsync(response -> {
                    if (response.statusCode() == Const.HTTP_OK) {
                        Repository.rpsSuccess.incrementAndGet();
                        Repository.incMoneySuccess();
                        var cash = JsonIterator.deserialize(response.body(), Integer[].class);
                        for (Integer i : cash) {
                            Repository.addMoney(i);
                        }
                    } else {
                        Repository.incMoneyError();
                        Repository.addMoneyRetry(response.request());
                    }
                }, responseEx);
    }

    public HttpResponse<byte[]> exploreBlocking(Area area) throws IOException, InterruptedException {
        return httpClient.send(createExploreRequest(area), HttpResponse.BodyHandlers.ofByteArray());
    }

    public void getNewPaidLicenseAsync(Integer cash) {
        httpClient.sendAsync(createPaidLicenseRequest(cash), HttpResponse.BodyHandlers.ofByteArray())
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

    public HttpResponse<byte[]> getNewFreeLicense() throws IOException, InterruptedException {
        return httpClient.send(newLicenseR, HttpResponse.BodyHandlers.ofByteArray());
    }

    public void digAsync(DigFull digFull) {
        httpClient.sendAsync(createDigRequest(digFull.getDigRq()), HttpResponse.BodyHandlers.ofByteArray())
                .thenAcceptAsync(response -> handleDigResponse(response, digFull));
    }

    public Explored doExplore(Area area) {
        while (true) {
            try {
                var response = exploreBlocking(area);
                if (response.statusCode() == Const.HTTP_OK) {
                    Repository.rpsSuccess.incrementAndGet();
                    Repository.incExplorerSuccess();
                    return JsonIterator.deserialize(response.body(), Explored.class);
                }
            } catch (Exception e) {
            }
        }
    }


    public void exploreMain(Area area) {
        try {
            var response = httpClient.send(createExploreRequest(area), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == Const.HTTP_OK) {
                Repository.rpsSuccess.incrementAndGet();
                var explored = JsonIterator.deserialize(response.body(), Explored.class);
                if (explored.getAmount() > 2) {
                    Repository.exploredMainDone.incrementAndGet();
                    Repository.exploredAreasMain.put(explored);
                } else {
                    Repository.skippedMain.incrementAndGet();
                }
            } else {
                Repository.incExplorerError();
            }
        } catch (Exception e) {
            Repository.incExplorerError();
        }
    }
}
