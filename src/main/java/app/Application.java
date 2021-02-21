package app;

import app.client.Client;
import app.client.Const;

import java.net.URISyntaxException;

public class Application {
    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        //var address = System.getenv("ADDRESS");
        var address = "localhost";
        System.err.println("ADDRESS = " + address);

        var port = 8000;//Integer.parseInt(System.getenv("Port"));
        System.err.println("Port = " + port);

        new Application(address, port).run();

    }

    private void run() {
        System.err.println("Client has been started");
        waitingForServer();
        System.err.println("Server has been started");
        try {
            for (int i = 0; i < 10; i++) {
                var response = client.getNewLicense();
                System.err.println(response.body());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
