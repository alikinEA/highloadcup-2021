package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.*;
import com.jsoniter.JsonIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Application {
    private static final int STEP = 3;
    public static final int GRABTIEFE = 11;

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        logger.error("Step = " + STEP + " GRABTIEFE = " + GRABTIEFE);
        var address = System.getenv("ADDRESS");

        logger.error("ADDRESS = " + address);

        var port = 8000;
        logger.error("Port = " + port);

        new Application(address, port).run();

    }

    private void run() {
        logger.error("Client has been started");
        waitingForServer();
        runBackgroundTasks();
        runDigger();


        Area area = new Area(0,0,3300,3300);//bestExplored.getArea();
        logger.error("start area = " + area);
        try {
            //logger.error("Single = " + client.exploreBlocking(area).body());
            for (int x = area.getPosX(); x < area.getPosX() + area.getSizeX(); x++) {
                for (int y = area.getPosY(); y < area.getPosY() + area.getSizeY(); y = y + STEP) {
                    Thread.sleep(1);
                    client.explore(new Area(x, y, 1, STEP));
                    /*var response = client.exploreBlocking(new Area(x, y, 1, 1));
                    var explored = JsonIterator.deserialize(response.body(), Explored.class);
                    logger.error("Explored = " + explored + Repository.getActionsInfo());
                    if (explored.getAmount() > 0) {
                        Repository.addExplored(explored);
                    }*/
                }
            }
        } catch (Exception e) {
            logger.error("Error", e);
        }

    }

    private void tryToGetMoney() {
        var moneyRetry = Repository.pollMoneyRetry();
        if (moneyRetry != null) {
            client.getMyMoney(moneyRetry);
            Repository.decrementMoneyError();
        }
    }

    private void runBackgroundTasks() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            tryToGetMoney();
            tryToGetLicense();
            tryToExploreRetry();
            logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("License receiver has been started");
    }

    private void tryToExploreRetry() {
        var exploreRequest = Repository.pollExploreRetry();
        if (exploreRequest != null) {
            client.explore(exploreRequest);
        }
    }

    private void tryToGetLicense() {
        try {
            var cash = Repository.pollMoney();
            if (cash != null) {
                try {
                    var response = client.getNewPaidLicense(cash);
                    if (response.statusCode() != Const.HTTP_OK) {
                        Repository.addMoney(cash);
                    } else {
                        var license = JsonIterator.deserialize(response.body(), License.class);
                        Repository.putLicenseNew(license);
                    }
                } catch (Exception e) {
                    Repository.addMoney(cash);
                }
            } else {
                var response = client.getNewFreeLicense();
                if (response.statusCode() == Const.HTTP_OK) {
                    var license = JsonIterator.deserialize(response.body(), License.class);
                    Repository.putLicenseNew(license);
                }
            }
        } catch (Exception e) {
            Repository.incLicenseErrors();
            //logger.error("license error = ", e);
        }
    }

    private void runDigger() {
        ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.submit(() -> {
            while (true) {
                var digFull = Repository.pollDugFull();
                if (digFull != null) {
                    var license = Repository.takeLicense();
                    digFull.getDigRq().setLicenseID(license.getId());
                    digFull.setLicense(license);
                    //logger.error("Dug one more time = " + digFull + Repository.getActionsInfo());
                    client.digBlocking(digFull);
                } else {
                    var license = Repository.takeLicense();
                    var explored = Repository.takeExplored();
                    var exploredArea = explored.getArea();
                    //logger.error("Take license = " + license);
                    var digRq = new DigRq(license.getId(), exploredArea.getPosX(), exploredArea.getPosY(), 1);
                    client.digBlocking(new DigFull(digRq, explored.getAmount(), 0, license));
                }
            }
        });
        logger.error("Digger has been started");
    }

    private Collection<Explored> explore5(int quantity) {
        int limit = 50_000;
        Collection<Explored> result = new ArrayList<>();
        Area area = new Area(0,0,3500,3500);//findBestPlace().getArea();
        logger.error("start area = " + area);
        try {
            int count = 0;
            //logger.error("Single = " + client.exploreBlocking(area).body());
            for (int x = area.getPosX(); x < area.getPosX() + area.getSizeX(); x++) {
                for (int y = area.getPosY(); y < area.getPosY() + area.getSizeY(); y = y + quantity) {
                    var response = client.exploreBlocking(new Area(x, y, 1, quantity));
                    if (response.statusCode() == Const.HTTP_OK) {
                        count++;
                        if (count == limit) {
                            return result.stream()
                                    .sorted(Comparator.comparing(Explored::getAmount).reversed())
                                    .collect(Collectors.toList());
                        }
                        var explored = JsonIterator.deserialize(response.body(), Explored.class);
                        if (explored.getAmount() > 1) {
                            result.add(explored);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error", e);
        }
        return result;
    }

    private void waitingForServer() {
        while(true) {
            try {
                Thread.sleep(10);
                var response = client.getNewFreeLicense();
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
}
