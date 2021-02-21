package app.client;

import app.client.models.License;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Repository {
    private static final BlockingQueue<License> freeLicenses = new LinkedBlockingQueue<>(10);

    public static License takeFreeLicense() throws InterruptedException {
        return freeLicenses.take();
    }

    public static void addFreeLicense(License license) throws InterruptedException {
        freeLicenses.put(license);
    }
}
