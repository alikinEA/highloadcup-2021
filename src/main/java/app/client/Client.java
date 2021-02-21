package app.client;

import app.client.models.DigRq;
import app.client.models.License;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsoniterSpi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Client {
    private final String url;

    private final HttpRequest healCheckR;
    private final HttpRequest licensesR;
    private final HttpRequest newLicenseR;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

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

    public HttpResponse<String> dig(License license, int posX, int posY, int depth) throws URISyntaxException, IOException, InterruptedException {
        var digR = new DigRq(license.getId(), posX, posY, depth);
        return httpClient.send(createDigRequest(digR), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest createDigRequest(DigRq digRq) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url + "/dig"))
                .headers(Const.CONTENT_TYPE, Const.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(JsonStream.serialize(digRq)))
                .build();
    }
}
