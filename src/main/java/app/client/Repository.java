package app.client;

import app.client.models.Explored;
import app.client.models.License;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Repository {
    private static final BlockingQueue<License> licenses = new LinkedBlockingDeque<>(9);
    private static final BlockingQueue<Explored> exploredAreas = new LinkedBlockingQueue<>(1000);
    private static final AtomicInteger digSuccess = new AtomicInteger(0);
    private static final AtomicInteger digError = new AtomicInteger(0);
    private static final AtomicInteger explorerSuccess = new AtomicInteger(0);

    public static License takeLicense() {
        try {
            return licenses.take();
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

    public static Explored takeExplored() {
        try {
            return exploredAreas.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("takeExplored error", e);
        }
    }

    public static void addExplored(Explored explored) {
        exploredAreas.add(explored);
    }

    public static int getExploredSize() {
        return exploredAreas.size();
    }

    public static String getActionsInfo() {
        return "Actions info: DigSuccess = " + digSuccess.get()
                + " DigError = " + digError.get()
                + " ExplorerSuccess = " + explorerSuccess.get()
                + " Explored size = " + Repository.getExploredSize()
                + " Licenses size = " + licenses.size();
    }

    public static int incDigSuccess() {
        return digSuccess.incrementAndGet();
    }

    public static int incDigError() {
        return digError.incrementAndGet();
    }

    public static int incExplorerSuccess() {
        return explorerSuccess.incrementAndGet();
    }

}
