package app.client;

import app.client.models.Area;
import app.client.models.Explored;
import app.client.models.License;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class Repository {
    private static final BlockingQueue<Area> areas = new LinkedBlockingQueue<>(10_000);
    private static final BlockingQueue<License> licenses = new LinkedBlockingDeque<>(9);
    private static final BlockingQueue<Explored> exploredAreas = new LinkedBlockingQueue<>();

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

    public static void putExplored(Explored explored) {
        try {
            exploredAreas.put(explored);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Area takeArea() {
        try {
            return areas.take();
        } catch (InterruptedException e) {
            throw new RuntimeException("takeArea error", e);
        }
    }

    public static void putArea(Area area) {
        try {
            areas.put(area);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
