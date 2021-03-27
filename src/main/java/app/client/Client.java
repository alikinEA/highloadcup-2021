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

    private HttpRequest createDigRequest(Dig dig) {
        return HttpRequest.newBuilder()
                .uri(digURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(dig)))
                .build();
    }

    private HttpRequest createExploreRequest(Area area) {
        return HttpRequest.newBuilder()
                .uri(exploreURI)
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(area)))
                .build();
    }

    private void handleDigResponse(HttpResponse<byte[]> response, Dig dig) {
        if (response.statusCode() == Const.HTTP_OK || response.statusCode() == Const.HTTP_NOT_FOUND) {
            Repository.rpsSuccess.incrementAndGet();
            var license = dig.getLicense();
            var amount = dig.getAmount();

            license.setDigAllowed(license.getDigAllowed() - 1);
            dig.setDepth(dig.getDepth() + 1);

            if (response.statusCode() == Const.HTTP_OK) {
                dig.setCurrentAmount(dig.getCurrentAmount() + 1);
                Repository.incDigSuccess();
                if (dig.getDepth() > 3) {
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
            if (dig.getDepth() == Application.GRABTIEFE && dig.getCurrentAmount() < amount) {
                Repository.incTreasureNotFound();
            }

            if (dig.getCurrentAmount() < amount) {
                if (license.getDigAllowed() > 0) {
                    digAsync(dig);
                } else {
                    Repository.addDug(dig);
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
        if (Repository.wallet.remainingCapacity() == 0) {
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding());
        } else {
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                    .thenAcceptAsync(response -> {
                        try {
                            if (response.statusCode() == Const.HTTP_OK) {
                                Repository.rpsSuccess.incrementAndGet();
                                Repository.incMoneySuccess();
                                var iterator = JsonIterator.parse(response.body());
                                while (iterator.readArray()) {
                                    Repository.addMoney(iterator.readInt());
                                }
                            } else {
                                Repository.incMoneyError();
                                Repository.addMoneyRetry(response.request());
                            }
                        } catch (Exception e) {
                            Repository.incMoneyError();
                        }
                    }, responseEx);
        }
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

    public void digAsync(Dig dig) {
        httpClient.sendAsync(createDigRequest(dig), HttpResponse.BodyHandlers.ofByteArray())
                .thenAcceptAsync(response -> handleDigResponse(response, dig));
    }

    public Explored doExplore(Area area) {
        while (true) {
            try {
                var response = exploreBlocking(area);
                if (response.statusCode() == Const.HTTP_OK) {
                    Repository.rpsSuccess.incrementAndGet();
                    Repository.incExplorerSuccess();
                    return JsonIterator.deserialize(response.body(), Explored.class);
                } else {
                    Repository.incExplorerError();
                }
            } catch (Exception e) {
                Repository.incExplorerError();
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
