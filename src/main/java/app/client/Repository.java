package app.client;

import app.client.models.License;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Repository {
    private static final BlockingQueue<License> freeLicenses = new LinkedBlockingQueue<>(10);

    public static License removeFreeLicense() {
        return freeLicenses.poll();
    }

    public static boolean addFreeLicense(License license) {
        return freeLicenses.add(license);
    }
}
