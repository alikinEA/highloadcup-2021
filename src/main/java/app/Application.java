package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.License;
import com.jsoniter.JsonIterator;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {
    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        var address = System.getenv("ADDRESS");
        //var address = "localhost";
        System.err.println("ADDRESS = " + address);

        var port = 8000;//Integer.parseInt(System.getenv("Port"));
        System.err.println("Port = " + port);

        new Application(address, port).run();

    }

    private void run() {
        System.err.println("Client has been started");
        waitingForServer();
        System.err.println("Server has been started");
        runLicenseReceiver();
        System.err.println("License receiver has been started");

        try {
            for (int i = 0; i <= 3500; i++) {
                for (int j = 0; j <= 3500; j++) {
                    var license = Repository.takeFreeLicense();
                    client.dig(license, i, j, 1);
                    client.dig(license, i, j, 2);
                    client.dig(license, i, j, 3);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void runLicenseReceiver() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            while (true) {
                var response = client.getNewLicense();
                if (response.statusCode() == Const.HTTP_OK) {
                    //System.err.println("New license has been received = " + response.body());
                    Repository.addFreeLicense(JsonIterator.deserialize(response.body(), License.class));
                }
            }
        });
    }

    private void waitingForServer() {
        try {
            Thread.sleep(1000);
            var response = client.getLicenses();
            if (response.statusCode() != Const.HTTP_OK) {
                this.waitingForServer();
            }
        } catch (Exception e) {
            this.waitingForServer();
        }
    }
}
