package app;

import app.client.Client;
import app.client.Const;
import app.client.Repository;
import app.client.models.*;
import com.jsoniter.JsonIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
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
        runDigger();

        try {
            //logger.error("Single = " + client.exploreBlocking(area).body());
            var area = new Area(0, 0, 1, STEP63);
            for (int x = 0; x < 3100; x++) {
                for (int y = 0; y < 3100; y = y + STEP63) {
                    area.setPosX(x);
                    area.setPosY(y);
                    client.explore63(area);
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
        }, 1, 1, TimeUnit.MILLISECONDS);
        logger.error("RunBackgroundMoneyRetry has been started");
    }

    private void runBackgroundExplore63() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Runnable explore63Task = () -> {
            logger.error("explore63Task has been started");
            while (true) {
                try {
                    Explored explore63 = Repository.exploredAreas63.take();
                    int summ = 0;
                    var area = explore63.getArea();
                    int startY = area.getPosY();
                    area.setSizeY(STEP3);
                    for (int i = 0; i < 63; i = i + STEP3) {
                        area.setPosY(startY + i);
                        var explored3 = client.doExplore(area);
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
            }
        };
        executorService.execute(explore63Task);
        executorService.execute(explore63Task);
    }


    private void findTh3(Explored explored3) {
        int summ = 0;
        var area = explored3.getArea();
        int startY = area.getPosY();
        area.setSizeY(1);
        for (int i = 0; i < 2; i++) {
            area.setPosY(startY + i);
            var explored1 = client.doExplore(area);
            summ = summ + explored1.getAmount();
            if (explored1.getAmount() > 0) {
                Repository.addExplored(explored1);
            }

            if (summ == explored3.getAmount()) {
                return;
            }
        }

        area.setPosY(startY + 2);
        Repository.addExplored(new Explored(area, explored3.getAmount() - summ));
        Repository.skip3Explore.incrementAndGet();
    }

    private void runBackgroundLicenses() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(() -> {
            if (Repository.licensesStore.size() < 8) {
                Repository.schedulerAttemptLicense.incrementAndGet();
                tryToGetLicense();
            }
            logger.error("Background stat = " + Repository.getActionsInfo());
            printGCStats();
        }, 1, 18, TimeUnit.MILLISECONDS);
        logger.error("runBackgroundLicenses receiver has been started");
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

    public void printGCStats() {
        /*logger.error("Heap" +  ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
        logger.error("NonHeap" + ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
        List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean bean: beans) {
            logger.error(bean.getName() + " : " + bean.getUsage());
        }*/

        for (GarbageCollectorMXBean bean: ManagementFactory.getGarbageCollectorMXBeans()) {
            logger.error(bean.getName() + " : " + bean.getCollectionCount() + " : " + bean.getCollectionTime());
        }
    }
}
