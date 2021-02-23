package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.Area;
import app.client.models.DigFull;
import app.client.models.DigRq;
import app.client.models.License;
import com.jsoniter.JsonIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {
    private static final int STEP = 1;
    //public static final int GRABTIEFE = 12;

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        logger.error("Step ver6 = " + STEP + " GRABTIEFE = " + 0 );
        var address = System.getenv("ADDRESS");
        //var address = "localhost";
        logger.error("ADDRESS = " + address);

        var port = 8000;//Integer.parseInt(System.getenv("Port"));
        logger.error("Port = " + port);

        new Application(address, port).run();

    }

    private void run() {
        logger.error("Client has been started");
        waitingForServer();
        runLicenseReceiver();
        runDigger();

        for (int i = 0; i < 3500; i++) {
            for (int j = 0; j < 3500; j = j + STEP) {
                try {
                    Thread.sleep(1);
                    tryToGetMoney();
                    var exploreRequest = Repository.pollExploreRetry();
                    if (exploreRequest != null) {
                        client.explore(exploreRequest);
                    } else {
                        client.explore(new Area(i, j, 1, STEP));
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void tryToGetMoney() {
        var moneyRetry = Repository.pollMoneyRetry();
        if (moneyRetry != null) {
            client.getMyMoney(moneyRetry);
            Repository.decrementMoneyError();
        }
    }

    private void runLicenseReceiver() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            Thread.currentThread().setPriority(10);
            while (true) {
                var response = client.getNewLicense();
                if (response.statusCode() == Const.HTTP_OK) {
                    var license = JsonIterator.deserialize(response.body(), License.class);
                    var digFull = Repository.pollDugFull();
                    if (digFull != null) {
                        digFull.getDigRq().setLicenseID(license.getId());
                        digFull.setLicense(license);
                        //logger.error("Dug one more time = " + digFull + Repository.getActionsInfo());
                        client.dig(digFull);
                    } else {
                        //logger.error("New license has been received = " + response.body());
                        Repository.putLicense(license);
                    }
                } else {
                    Repository.incLicenseErrors();
                }
            }
        });
        logger.error("License receiver has been started");
    }

    private void runDigger() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            Thread.currentThread().setPriority(9);
            while (true) {
                var explored = Repository.takeExplored();
                var exploredArea = explored.getArea();
                var license = Repository.takeLicense();
                //logger.error("Take license = " + license);
                var digRq = new DigRq(license.getId(), exploredArea.getPosX(), exploredArea.getPosY(), 1);
                var digFull = new DigFull(digRq, explored.getAmount(), 0, license);
                client.dig(digFull);
            }
        });
        logger.error("Digger has been started");
    }

    private void waitingForServer() {
        while(true) {
            try {
                Thread.sleep(10);
                var response = client.getNewLicense();
                if (response.statusCode() != Const.HTTP_OK) {
                    logger.error("Server has been started");
                    return;
                }
            } catch (Exception e) {

            }
        }
    }
}
