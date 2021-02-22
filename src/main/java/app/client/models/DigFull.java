package app.client.models;

import java.util.concurrent.atomic.AtomicInteger;

public class DigFull {
    private DigRq digRq;
    private int amount;
    private final AtomicInteger currentAmount;
    private License license;

    public DigFull() {
        this.currentAmount = new AtomicInteger(0);
    }

    public DigFull(DigRq digRq, int amount, int currentAmount, License license) {
        this.digRq = digRq;
        this.amount = amount;
        this.currentAmount = new AtomicInteger(currentAmount);
        this.license = license;
    }



    public DigRq getDigRq() {
        return digRq;
    }

    public void setDigRq(DigRq digRq) {
        this.digRq = digRq;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public AtomicInteger getCurrentAmount() {
        return currentAmount;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    @Override
    public String toString() {
        return "DigFull{" +
                "digRq=" + digRq +
                ", amount=" + amount +
                ", currentAmount=" + currentAmount.get() +
                ", license=" + license +
                '}';
    }
}
