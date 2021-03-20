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
    private static final int STEP_MAIN = 63;
    private static final int STEP3 = 3;
    public static final int GRABTIEFE = 11;

    private static Logger logger = LoggerFactory.getLogger(Client.class);

    private final Client client;

    public Application(String address, int port) throws URISyntaxException {
        this.client = new Client(address, port);
    }

    public static void main(String[] args) throws URISyntaxException {
        logger.error("Step = " + STEP_MAIN + " GRABTIEFE = " + GRABTIEFE);
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
        runBackgroundExploreMain();
        runDigger();

        try {
            //logger.error("Single = " + client.exploreBlocking(area).body());
            var area = new Area(0, 0, 1, STEP_MAIN);
            for (int x = 0; x < 3100; x++) {
                for (int y = 0; y < 3100; y = y + STEP_MAIN) {
                    area.setPosX(x);
                    area.setPosY(y);
                    client.exploreMain(area);
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

    private void runBackgroundExploreMain() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Runnable exploreMainTask = () -> {
            logger.error("explore63Task has been started");
            while (true) {
                try {
                    Explored exploreMain = Repository.exploredAreasMain.take();
                    int summ = 0;
                    var area = exploreMain.getArea();
                    int startY = area.getPosY();
                    area.setSizeY(STEP3);
                    for (int i = 0; i < STEP_MAIN - STEP3; i = i + STEP3) {
                        area.setPosY(startY + i);
                        var explored3 = client.doExplore(area);
                        summ = summ + explored3.getAmount();
                        if (explored3.getAmount() > 0) {
                            findTh3(explored3);
                        }

                        if (summ == exploreMain.getAmount()) {
                            break;
                        }
                    }

                    if (summ != exploreMain.getAmount()) {
                        Repository.skipLastMainExplore.incrementAndGet();
                        area.setPosY(startY + (STEP_MAIN - STEP3));
                        findTh3(new Explored(area, exploreMain.getAmount() - summ));
                    }

                } catch (InterruptedException e) {
                    Repository.incTreasureNotFound();
                }
            }
        };
        executorService.execute(exploreMainTask);
        executorService.execute(exploreMainTask);
    }


    private void findTh3(Explored explored3) {
        int summ = 0;
        var area = explored3.getArea();
        int startY = area.getPosY();
        area.setSizeY(1);
        for (int i = 0; i < STEP3 - 1; i++) {
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

        area.setPosY(startY + (STEP3 - 1));
        Repository.addExplored(new Explored(area, explored3.getAmount() - summ));
        Repository.skipLast3Explore.incrementAndGet();
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
        }, 1, 16, TimeUnit.MILLISECONDS);
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
