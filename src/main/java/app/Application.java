package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.*;
import com.jsoniter.JsonIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Application {
    private static final int STEP1 = 25;
    private static final int STEP2 = 5;
    private static final int STEP3 = 2;
    private static final int STEP4 = 3;
    public static final int GRABTIEFE = 11;

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        logger.error("Step = " + STEP1 + " GRABTIEFE = " + GRABTIEFE);
        var address = System.getenv("ADDRESS");

        logger.error("ADDRESS = " + address);

        var port = 8000;
        logger.error("Port = " + port);

        new Application(address, port).run();

    }

    private void run() {
        logger.error("Client has been started");
        waitingForServer();
        runBackgroundLicenses();
        runBackgroundMoneyRetry();
        runDigger();
        runBackgroundExplore25();

        try {
            //logger.error("Single = " + client.exploreBlocking(area).body());
            for (int x1 = 0; x1 < 3500; x1++) {
                for (int y1 = 0; y1 < 3500; y1 = y1 + STEP1) {
                    Thread.sleep(8);
                    client.exploreAsync25(new Area(x1, y1, 1, STEP1));
                }
            }
        } catch (Exception e) {
            Repository.explorerError.incrementAndGet();
            logger.error("Error", e);
        }

    }

    private void findTreasure5(Explored explored5) {
        var explored2_1 = doExplore(new Area(explored5.getArea().getPosX(), explored5.getArea().getPosY(), 1, 2));
        if (explored2_1.getAmount() == explored5.getAmount()) {
            findThreasure2(explored2_1);
        } else {
            var explored2_2 = doExplore(new Area(explored5.getArea().getPosX(), explored5.getArea().getPosY() + 2, 1, 2));
            if (explored2_1.getAmount() == explored5.getAmount()) {
                findThreasure2(explored2_1);
            } else {
                if (explored2_1.getAmount() == 0 && explored2_2.getAmount() == 0) {
                    client.exploreAsync(new Area(explored5.getArea().getPosX(), explored5.getArea().getPosY() + 4, 1, 1));
                } else {
                    if (explored2_1.getAmount() > 0) {
                        findThreasure2(explored2_1);
                    }
                    if (explored2_2.getAmount() > 0) {
                        findThreasure2(explored2_2);
                    }
                }
            }
        }
    }

    private void findThreasure2(Explored explored2) {
        var explored1_1 = doExplore(new Area(explored2.getArea().getPosX(), explored2.getArea().getPosY(), 1, 1));
        if (explored1_1.getAmount() == explored2.getAmount()) {
            Repository.addExplored(explored1_1);
        } else {
            if (explored1_1.getAmount() > 0) {
                Repository.addExplored(explored1_1);
            }
            client.exploreAsync(new Area(explored2.getArea().getPosX(), explored2.getArea().getPosY() + 1, 1, 1));
        }
    }

    private Explored doExplore(Area area) {
        while (true) {
            try {
                var responseF = client.exploreBlocking(area);
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

    private void runBackgroundMoneyRetry() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            try {
                client.getMyMoney(Repository.takeMoneyRetry());
                Repository.schedulerMoneyRetry.incrementAndGet();
                Repository.decrementMoneyError();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("License receiver has been started");
    }

    private void runBackgroundExplore25() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            try {
                Explored explore25 = Repository.takeExplore25();
                int summ = 0;
                for (int i = 0; i < 25; i = i + STEP2) {
                    var explored_5 = doExplore(new Area(explore25.getArea().getPosX(), explore25.getArea().getPosY() + i, 1, STEP2));
                    summ = summ + explored_5.getAmount();
                    if (explored_5.getAmount() > 0) {
                        findTreasure5(explored_5);
                    }
                    if (summ == explore25.getAmount()) {
                        break;
                    }
                }
                if (summ != explore25.getAmount()) {
                    Repository.incTreasureNotFound();
                }
            } catch (InterruptedException e) {
                Repository.incTreasureNotFound();
            }
            //logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("License receiver has been started");
    }

    private void runBackgroundLicenses() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            Repository.schedulerAttemptLicense.incrementAndGet();
            tryToGetLicense();
            logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 28, TimeUnit.MILLISECONDS);
        logger.error("License receiver has been started");
    }

    private void tryToGetLicense() {
        try {
            var cash = Repository.pollMoney();
            if (cash != null) {
                try {
                    client.getNewPaidLicenseAsync(cash);
                } catch (Exception e) {
                    Repository.addMoney(cash);
                    Repository.incLicenseErrors();
                    logger.error("license error1 = " + Repository.getActionsInfo(), e);
                }
            } else {
                var response = client.getNewFreeLicense();
                if (response.statusCode() == Const.HTTP_OK) {
                    Repository.rpsSuccess.incrementAndGet();
                    var license = JsonIterator.deserialize(response.body(), License.class);
                    Repository.putLicenseNew(license);
                }
            }
        } catch (Exception e) {
            Repository.incLicenseErrors();
            logger.error("license error2 = " + Repository.getActionsInfo(), e);
        }
    }

    private void runDigger() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            var digFull = Repository.pollDugFull();
            var license = Repository.takeLicense();
            if (digFull != null) {
                digFull.getDigRq().setLicenseID(license.getId());
                digFull.setLicense(license);
                //logger.error("Dug one more time = " + digFull + Repository.getActionsInfo());
                client.digAsync(digFull);
            } else {
                var explored = Repository.takeExplored();
                var exploredArea = explored.getArea();
                //logger.error("Take license = " + license);
                var digRq = new DigRq(license.getId(), exploredArea.getPosX(), exploredArea.getPosY(), 1);
                client.digAsync(new DigFull(digRq, explored.getAmount(), 0, license));
            }

        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("Digger has been started");
    }

    private void waitingForServer() {
        while(true) {
            try {
                Thread.sleep(10);
                var response = client.getNewFreeLicense();
                if (response.statusCode() == Const.HTTP_OK) {
                    Repository.rpsSuccess.incrementAndGet();
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
