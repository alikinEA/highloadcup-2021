package app.client;

import app.client.models.*;

import java.net.http.HttpRequest;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Repository {
    private static final BlockingQueue<DigFull> dugFull = new LinkedBlockingDeque<>();
    public static final BlockingQueue<License> licensesStore = new LinkedBlockingDeque<>();
    private static final BlockingQueue<HttpRequest> moneyRetry = new LinkedBlockingDeque<>();
    public static final BlockingQueue<Explored> exploredAreas1 = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Explored> exploredAreas2 = new LinkedBlockingQueue<>();
    public static  final PriorityBlockingQueue<Explored> exploredAreas63 = new PriorityBlockingQueue<>(30_000, Comparator.comparingInt(Explored::getAmount).reversed());
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

    public static final BlockingQueue<Integer> wallet = new LinkedBlockingDeque<>(1_000_000);
    private static final AtomicInteger paidLicenses = new AtomicInteger(0);
    private static final AtomicInteger freeLicenses = new AtomicInteger(0);
    public static final AtomicInteger licenseFull = new AtomicInteger(0);

    public static final AtomicInteger licenseAttempt = new AtomicInteger(0);
    public static final AtomicInteger schedulerAttemptLicense = new AtomicInteger(0);
    public static final AtomicInteger schedulerMoneyRetry = new AtomicInteger(0);

    public static final AtomicInteger explorerError = new AtomicInteger(0);
    public static final AtomicInteger rpsSuccess = new AtomicInteger(0);
    public static final AtomicInteger explored63Done = new AtomicInteger(0);
    public static final AtomicInteger skipped63 = new AtomicInteger(0);


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
                + " DugFull = " + dugFull.size()
                + " LicenseError size = " + licenseError.get()
                + " LicensesStore = " + licensesStore.size()
                + " PaidLicenses = " + paidLicenses.get()
                + " FreeLicenses = " + freeLicenses.get()
                + " licenseFull " + licenseFull.get()
                + " getLicenseAttempt " + licenseAttempt.get()
                + " schedulerMoneyRetry " + schedulerMoneyRetry.get()
                + " schedulerAttemptLicense " + schedulerAttemptLicense.get()
                + " rpsSuccess " + rpsSuccess.get()
                + " exploredAreas63 = " + exploredAreas63.size()
                + " explored63Done = " + explored63Done.get()
                + " skipped63 = " + skipped63.get();
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
