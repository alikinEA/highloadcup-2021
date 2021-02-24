package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.*;
import com.jsoniter.JsonIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanRegistration;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Application {
    private static final int STEP = 2;
    public static final int GRABTIEFE = 11;

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        logger.error("Step ver6 = " + STEP + " GRABTIEFE = " + GRABTIEFE);
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

        for (int i = 3490; i > 3400; i--) {
            client.explore(new Area(3490, i, 1, 1));
        }

        Explored bestExplored = null;
        Explored place ;//findBestPlace();
        /*Explored line1 = findBestLine1();
        Explored line2 = findBestLine2();
        if (line1.getAmount() > line2.getAmount() && line1.getAmount() > place.getAmount()) {
            bestExplored = line1;
        } else if (line2.getAmount() > line1.getAmount() && line2.getAmount() > place.getAmount()) {
            bestExplored = line2;
        } else if (place.getAmount() > line1.getAmount() && place.getAmount() > line2.getAmount()) {
            bestExplored = place;
        }*/

       // bestExplored = place;
        Area area = new Area(0,0,3500,3500);//bestExplored.getArea();
        logger.error("Run area = " + bestExplored);
        for (int i = area.getPosX(); i < area.getPosX() + area.getSizeX(); i++) {
            for (int j = area.getPosY(); j < area.getPosY() + area.getSizeY(); j = j + STEP) {
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
        while (true) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tryToGetMoney();
        }
    }

    private Explored findBestLine1() {
        int size = 14;
        Area area = new Area(0,0,0,3500);
        Explored bestExplored = new Explored(area, 0);
        for (int i = 0; i < 3500; i = i + size) {
            area.setPosX(i);
            area.setPosY(0);
            area.setSizeX(size);
            area.setSizeY(3500);
            try {
                HttpResponse<String> response = client.exploreBlocking(area);
                var explored = JsonIterator.deserialize(response.body(), Explored.class);
                if (explored.getAmount() > bestExplored.getAmount()) {
                    //logger.error("Greater exp line = " + explored);
                    bestExplored = explored;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.error("Best expl line1 = " + bestExplored);
        return bestExplored;
    }

    private Explored findBestLine2() {
        int size = 14;
        Area area = new Area(0,0,0,3500);
        Explored bestExplored = new Explored(area, 0);
        for (int i = 0; i < 3500; i = i + size) {
            area.setPosX(0);
            area.setPosY(i);
            area.setSizeX(3500);
            area.setSizeY(size);
            try {
                HttpResponse<String> response = client.exploreBlocking(area);
                var explored = JsonIterator.deserialize(response.body(), Explored.class);
                if (explored.getAmount() > bestExplored.getAmount()) {
                    //logger.error("Greater exp line = " + explored);
                    bestExplored = explored;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.error("Best expl line2 = " + bestExplored);
        return bestExplored;
    }

    private Explored findBestPlace() {
        int size = 700;
        Area area = new Area(0,0,3500,3500);
        Explored bestExplored = new Explored(area, 0);
        for (int i = 0; i < 3500; i = i + size) {
            for (int j = 0; j < 3500; j = j + size) {
                area.setPosX(i);
                area.setPosY(j);
                area.setSizeX(size);
                area.setSizeY(size);
                try {
                    var response = client.exploreBlocking(area);
                    var explored = JsonIterator.deserialize(response.body(), Explored.class);
                    if (explored.getAmount() > bestExplored.getAmount()) {
                        logger.error("Greater exp = " + explored);
                        bestExplored = explored;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        logger.error("Best expl = " + bestExplored);
        return bestExplored;
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
                    //logger.error("Getting license success = " + Repository.getActionsInfo());
                    var license = JsonIterator.deserialize(response.body(), License.class);
                    Repository.putLicenseNew(license);
                } else {
                    logger.error("Getting license error = " + response + Repository.getActionsInfo());
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
                var digFull = Repository.pollDugFull();
                if (digFull != null) {
                    var license = Repository.takeLicense();
                    digFull.getDigRq().setLicenseID(license.getId());
                    digFull.setLicense(license);
                    //logger.error("Dug one more time = " + digFull + Repository.getActionsInfo());
                    client.dig(digFull);
                    Thread.sleep(1);
                } else {
                    var license = Repository.takeLicense();
                    var explored = Repository.takeExplored();
                    var exploredArea = explored.getArea();
                    //logger.error("Take license = " + license);
                    var digRq = new DigRq(license.getId(), exploredArea.getPosX(), exploredArea.getPosY(), 1);
                    client.dig(new DigFull(digRq, explored.getAmount(), 0, license));
                    Thread.sleep(1);
                }
            }
        });
        logger.error("Digger has been started");
    }

    private void waitingForServer() {
        while(true) {
            try {
                Thread.sleep(10);
                var response = client.getNewLicense();
                if (response.statusCode() == Const.HTTP_OK) {
                    var license = JsonIterator.deserialize(response.body(), License.class);
                    Repository.putLicenseNew(license);
                    logger.error("Server has been started");
                    return;
                }
            } catch (Exception e) {

            }
        }
    }
}
