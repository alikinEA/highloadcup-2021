package app.client;

import app.client.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Repository {
    private static Logger logger = LoggerFactory.getLogger(Repository.class);

    private static final BlockingQueue<DigFull> dugFull = new LinkedBlockingDeque<>();
    public static final BlockingQueue<License> licensesStore = new LinkedBlockingDeque<>();
    private static final BlockingQueue<HttpRequest> moneyRetry = new LinkedBlockingDeque<>();
    public static final BlockingQueue<Explored> exploredAreas1 = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Explored> exploredAreas2 = new LinkedBlockingQueue<>();
    private static final AtomicInteger digSuccess = new AtomicInteger(0);
    private static final AtomicInteger digMiss = new AtomicInteger(0);
    private static final AtomicInteger digError = new AtomicInteger(0);
    private static final AtomicInteger explorerSuccess = new AtomicInteger(0);
    private static final AtomicInteger moneyError = new AtomicInteger(0);
    private static final AtomicInteger moneySuccess = new AtomicInteger(0);
    private static final AtomicInteger treasureNotFound = new AtomicInteger(0);
    private static final AtomicInteger licenseError = new AtomicInteger(0);
    private static final AtomicInteger richPlaces = new AtomicInteger(0);
    public static final AtomicInteger richPlacesDone = new AtomicInteger(0);
    private static final AtomicInteger richArea = new AtomicInteger(0);

    public static final BlockingQueue<Integer> wallet = new LinkedBlockingDeque<>(1_000_000);
    private static final AtomicInteger paidLicenses = new AtomicInteger(0);
    private static final AtomicInteger freeLicenses = new AtomicInteger(0);

    public static final AtomicInteger skipped25 = new AtomicInteger(0);
    public static final AtomicInteger skipped5 = new AtomicInteger(0);
    public static final AtomicInteger skipped50 = new AtomicInteger(0);
    public static final AtomicInteger skipped5_1 = new AtomicInteger(0);
    public static final AtomicInteger licenseFull = new AtomicInteger(0);

    public static final AtomicInteger licenseAttempt = new AtomicInteger(0);
    public static final AtomicInteger schedulerAttemptLicense = new AtomicInteger(0);
    public static final AtomicInteger schedulerAttemptMoney = new AtomicInteger(0);

    public static License takeLicense() {
        try {
            return licensesStore.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("takeLicense error", e);
        }
    }

    public static void putLicenseNew(License license) {
        putLicense(license);
        if (licensesStore.size() > 8) {
            logger.error("License store is full (8)");
        }
        if (license.getDigAllowed() == 5) {
            paidLicenses.incrementAndGet();
        } else {
            freeLicenses.incrementAndGet();
        }
    }

    public static void putLicense(License license) {
        try {
            licensesStore.put(license);
            if (licensesStore.size() == 10) {
                Repository.licenseFull.incrementAndGet();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("putLicense error", e);
        }
    }

    public static void addMoney(Integer cash) {
        wallet.add(cash);
    }

    public static Integer pollMoney() {
        return wallet.poll();
    }

    public static Explored takeExplored() {
        try {
            if (exploredAreas2.size() != 0) {
                return exploredAreas2.take();
            } else {
                return exploredAreas1.take();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("takeExplored error", e);
        }
    }

    public static void addExplored(Explored explored) {
        if (explored.getAmount() == 0) {
            incTreasureNotFound();
        }
        if (explored.getAmount() == 1) {
            if (exploredAreas1.size() < 100) {
                exploredAreas1.add(explored);
            }
        } else {
            richPlaces.incrementAndGet();
            exploredAreas2.add(explored);
        }
    }

    public static String getActionsInfo() {
        return "Actions info: DigSuccess = " + digSuccess.get()
                + " DigError = " + digError.get()
                + " DigMiss = " + digMiss.get()
                + " Rich places = " + richPlaces.get()
                + " Rich area = " + richArea.get()
                + " RichPlacesDone" + richPlacesDone.get()
               // + " MoneyError = " + moneyError.get()
                + " MoneySuccess = " + moneySuccess.get()
                + " Wallet size = " + wallet.size()
                + " TreasureNotFound = " + treasureNotFound.get()
                + " ExplorerSuccess = " + explorerSuccess.get()
                + " Explored size1 = " + exploredAreas1.size()
                + " Explored size2 = " + exploredAreas2.size()
                + " DugFull = " + dugFull.size()
                + " LicenseError size = " + licenseError.get()
                + " LicensesStore = " + licensesStore.size()
                + " PaidLicenses = " + paidLicenses.get()
                + " FreeLicenses = " + freeLicenses.get()
                + " Skipped25 " + skipped25.get()
                + " Skipped5 " + skipped5.get()
                + " Skipped5_1 " + skipped5_1.get()
                + " skipped50 " + skipped50.get()
                + " licenseFull " + licenseFull.get()
                + " getLicenseAttempt " + licenseAttempt.get()
                + " schedulerAttemptMoney " + schedulerAttemptMoney.get()
                + " schedulerAttemptLicense " + schedulerAttemptLicense.get();
    }

    public static int incDigSuccess() {
        return digSuccess.incrementAndGet();
    }

    public static int incDigError() {
        return digError.incrementAndGet();
    }

    public static int incDigMiss() {
        return digMiss.incrementAndGet();
    }

    public static int incMoneySuccess() {
        return moneySuccess.incrementAndGet();
    }

    public static int incMoneyError() {
        return moneyError.incrementAndGet();
    }

    public static int incExplorerSuccess() {
        return explorerSuccess.incrementAndGet();
    }

    public static DigFull pollDugFull() {
        return dugFull.poll();
    }

    public static boolean addDugFull(DigFull fullDig) {
        return dugFull.add(fullDig);
    }

    public static int incTreasureNotFound() {
        return treasureNotFound.incrementAndGet();
    }

    public static void addMoneyRetry(HttpRequest request) {
        moneyRetry.add(request);
    }

    public static HttpRequest pollMoneyRetry() {
        return moneyRetry.poll();
    }

    public static int decrementMoneyError() {
        return moneyError.decrementAndGet();
    }

    public static int incLicenseErrors() {
        return licenseError.incrementAndGet();
    }

    public static void incRichArea() {
        richArea.incrementAndGet();
    }
}
