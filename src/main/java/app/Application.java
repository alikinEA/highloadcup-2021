package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.*;
import com.jsoniter.JsonIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application {
    private static final int STEP63 = 63;
    private static final int STEP3 = 3;
    public static final int GRABTIEFE = 11;

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        logger.error("Step = " + STEP63 + " GRABTIEFE = " + GRABTIEFE);
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
        runBackgroundExplore63();
        runBackgroundExplore63_2();
        runDigger();

        try {
            //logger.error("Single = " + client.exploreBlocking(area).body());
            for (int x1 = 0; x1 < 3100; x1++) {
                for (int y1 = 0; y1 < 3100; y1 = y1 + STEP63) {
                    client.exploreAsync63(new Area(x1, y1, 1, STEP63));
                }
            }
        } catch (Exception e) {
            Repository.explorerError.incrementAndGet();
            logger.error("Error", e);
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

    private void runBackgroundExplore63() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            try {
                Explored explore63 = Repository.exploredAreas63.take();
                int summ = 0;
                for (int i = 0; i < 63; i = i + STEP3) {
                    var explored3 = client.doExplore(new Area(explore63.getArea().getPosX(), explore63.getArea().getPosY() + i, 1, STEP3));
                    summ = summ + explored3.getAmount();
                    if (explored3.getAmount() > 0) {
                        findTh3(explored3);
                    }

                    if (summ == explore63.getAmount()) {
                        break;
                    }
                }

                if (summ != explore63.getAmount()) {
                    Repository.incTreasureNotFound();
                }
            } catch (InterruptedException e) {
                Repository.incTreasureNotFound();
            }
            //logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("License receiver has been started");
    }

    private void runBackgroundExplore63_2() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            try {
                Explored explore63 = Repository.exploredAreas63.take();
                int summ = 0;
                for (int i = 0; i < 63; i = i + STEP3) {
                    var explored3 = client.doExplore(new Area(explore63.getArea().getPosX(), explore63.getArea().getPosY() + i, 1, STEP3));
                    summ = summ + explored3.getAmount();
                    if (explored3.getAmount() > 0) {
                        findTh3(explored3);
                    }

                    if (summ == explore63.getAmount()) {
                        break;
                    }
                }

                if (summ != explore63.getAmount()) {
                    Repository.incTreasureNotFound();
                }
            } catch (InterruptedException e) {
                Repository.incTreasureNotFound();
            }
            //logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("License receiver has been started");
    }

    private void findTh3(Explored explored3) {
        int summ = 0;
        for (int i = 0; i < 3; i++) {
            var explored1 = client.doExplore(new Area(explored3.getArea().getPosX(), explored3.getArea().getPosY() + i, 1, 1));
            summ = summ + explored1.getAmount();
            if (explored1.getAmount() > 0) {
                Repository.addExplored(explored1);
            }

            if (summ == explored3.getAmount()) {
                break;
            }
        }
        if (summ != explored3.getAmount()) {
            Repository.incTreasureNotFound();
        }
    }

    private void runBackgroundLicenses() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            if (Repository.licensesStore.size() < 8) {
                Repository.schedulerAttemptLicense.incrementAndGet();
                tryToGetLicense();
            }
            logger.error("Background stat = " + Repository.getActionsInfo());
        }, 1, 18, TimeUnit.MILLISECONDS);
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
