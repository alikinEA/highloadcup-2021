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
    public static final AtomicInteger digRPS = new AtomicInteger(0);
    public static final AtomicInteger explorerRPS = new AtomicInteger(0);

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
}
