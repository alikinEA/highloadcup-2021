package app.client;

import app.client.models.Dig;
import app.client.models.Explored;
import app.client.models.License;

import java.net.http.HttpRequest;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Repository {
    private static final BlockingQueue<Dig> dug = new LinkedBlockingQueue<>();
    public static final BlockingQueue<License> licensesStore = new LinkedBlockingQueue<>();
    private static final BlockingQueue<HttpRequest> moneyRetry = new LinkedBlockingQueue<>();
    public static final BlockingQueue<Explored> exploredAreas1 = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Explored> exploredAreas2 = new LinkedBlockingQueue<>();
    public static  final BlockingQueue<Explored> exploredAreasMain = new LinkedBlockingQueue<>(2);
    public static final BlockingQueue<Integer> wallet = new LinkedBlockingQueue<>(30_000);

    private static final AtomicInteger digSuccess = new AtomicInteger(0);
    private static final AtomicInteger digMiss = new AtomicInteger(0);
    private static final AtomicInteger digError = new AtomicInteger(0);
    private static final AtomicInteger explorerSuccess = new AtomicInteger(0);
    private static final AtomicInteger moneyError = new AtomicInteger(0);
    private static final AtomicInteger moneySuccess = new AtomicInteger(0);
    private static final AtomicInteger treasureNotFound = new AtomicInteger(0);
    private static final AtomicInteger licenseError = new AtomicInteger(0);
    private static final AtomicInteger richPlaces = new AtomicInteger(0);
    private static final AtomicInteger places = new AtomicInteger(0);

    private static final AtomicInteger paidLicenses = new AtomicInteger(0);
    private static final AtomicInteger freeLicenses = new AtomicInteger(0);
    public static final AtomicInteger licenseFull = new AtomicInteger(0);

    public static final AtomicInteger licenseAttempt = new AtomicInteger(0);
    public static final AtomicInteger schedulerAttemptLicense = new AtomicInteger(0);
    public static final AtomicInteger schedulerMoneyRetry = new AtomicInteger(0);

    public static final AtomicInteger explorerError = new AtomicInteger(0);
    public static final AtomicInteger rpsSuccess = new AtomicInteger(0);
    public static final AtomicInteger exploredMainDone = new AtomicInteger(0);
    public static final AtomicInteger skippedMain = new AtomicInteger(0);
    public static final AtomicInteger skipLast3Explore = new AtomicInteger(0);
    public static final AtomicInteger skipLastMainExplore = new AtomicInteger(0);

    public static final AtomicInteger skipTreasure = new AtomicInteger(0);

    public static License takeLicense() {
        try {
            return licensesStore.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("takeLicense error", e);
        }
    }

    public static void putLicenseNew(License license) {
        putLicense(license);
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
            places.incrementAndGet();
            exploredAreas1.add(explored);
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
                + " Places = " + places.get()
               // + " MoneyError = " + moneyError.get()
                + " MoneySuccess = " + moneySuccess.get()
                + " Wallet size = " + wallet.size()
                + " TreasureNotFound = " + treasureNotFound.get()
                + " ExplorerSuccess = " + explorerSuccess.get()
                + " ExplorerErrors = " + explorerError.get()
                + " Explored size1 = " + exploredAreas1.size()
                + " Explored size2 = " + exploredAreas2.size()
                + " Dug = " + dug.size()
                + " LicenseError size = " + licenseError.get()
                + " LicensesStore = " + licensesStore.size()
                + " PaidLicenses = " + paidLicenses.get()
                + " FreeLicenses = " + freeLicenses.get()
                + " licenseFull " + licenseFull.get()
                + " getLicenseAttempt " + licenseAttempt.get()
                + " schedulerMoneyRetry " + schedulerMoneyRetry.get()
                + " schedulerAttemptLicense " + schedulerAttemptLicense.get()
                + " rpsSuccess " + rpsSuccess.get()
                + " exploredAreasMain = " + exploredAreasMain.size()
                + " exploredMainDone = " + exploredMainDone.get()
                + " skippedMain = " + skippedMain.get()
                + " skipLast3Explore =" + skipLast3Explore.get()
                + " skipLastMainExplore =" + skipLastMainExplore.get()
                + " skipTreasure =" + skipTreasure.get();
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

    public static Dig pollDug() {
        return dug.poll();
    }

    public static boolean addDug(Dig dig) {
        return dug.add(dig);
    }

    public static int incTreasureNotFound() {
        return treasureNotFound.incrementAndGet();
    }

    public static void addMoneyRetry(HttpRequest request) {
        moneyRetry.add(request);
    }

    public static HttpRequest takeMoneyRetry() throws InterruptedException {
        return moneyRetry.take();
    }

    public static int decrementMoneyError() {
        return moneyError.decrementAndGet();
    }

    public static int incLicenseErrors() {
        return licenseError.incrementAndGet();
    }

    public static void incExplorerError() {
        explorerError.incrementAndGet();
    }
}
