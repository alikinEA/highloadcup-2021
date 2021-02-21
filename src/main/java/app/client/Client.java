package app.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Client {
    private final HttpRequest healCheckR;
    private final HttpRequest licensesR;
    private final HttpRequest newLicenseR;
    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public Client(String address, int port) throws URISyntaxException {
        String url = "http://" + address + ":" + port;
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
}
