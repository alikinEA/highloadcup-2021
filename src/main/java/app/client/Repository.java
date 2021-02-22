package app.client;

import app.client.models.DigFull;
import app.client.models.DigRq;
import app.client.models.Explored;
import app.client.models.License;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Repository {
    private static final BlockingQueue<DigFull> dugFull = new LinkedBlockingDeque<>();
    private static final BlockingQueue<License> licenses = new LinkedBlockingDeque<>(9);
    private static final BlockingQueue<License> licensesUsed = new LinkedBlockingDeque<>(9);
    private static final BlockingQueue<Explored> exploredAreas1 = new LinkedBlockingQueue<>(200);
    private static final BlockingQueue<Explored> exploredAreas2 = new LinkedBlockingQueue<>(100);
    private static final AtomicInteger digSuccess = new AtomicInteger(0);
    private static final AtomicInteger digMiss = new AtomicInteger(0);
    private static final AtomicInteger digError = new AtomicInteger(0);
    private static final AtomicInteger explorerSuccess = new AtomicInteger(0);
    private static final AtomicInteger moneyError = new AtomicInteger(0);
    private static final AtomicInteger moneySuccess = new AtomicInteger(0);
    private static final AtomicInteger treasureNotFound = new AtomicInteger(0);

    public static License takeLicense() {
        try {
            if (licensesUsed.size() == 0) {
                return licenses.take();
            } else {
                return licensesUsed.take();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("takeLicense error", e);
        }
    }

    public static void putLicense(License license) {
        try {
            licenses.put(license);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void putUsedLicense(License license) {
        try {
            licensesUsed.put(license);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
        if (explored.getAmount() == 1) {
            if (exploredAreas1.size() < 100) {
                exploredAreas1.add(explored);
            }
        } else if (explored.getAmount() > 1) {
            exploredAreas2.add(explored);
        }
    }

    public static String getActionsInfo() {
        return "Actions info: DigSuccess = " + digSuccess.get()
                + " DigError = " + digError.get()
                + " DigMiss = " + digMiss.get()
                + " MoneyError = " + moneyError.get()
                + " MoneySuccess = " + moneySuccess.get()
                + " TreasureNotFound = " + treasureNotFound.get()
                + " ExplorerSuccess = " + explorerSuccess.get()
                //+ " Explored size1 = " + exploredAreas1.size()
                //+ " Explored size2 = " + exploredAreas2.size()
                + " DugFull = " + dugFull.size()
                + " Licenses size = " + licenses.size()
                + " Licenses used size = " + licensesUsed.size();
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
}
