package app.client.models;

import com.jsoniter.annotation.JsonIgnore;

public class Dig {
    @JsonIgnore
    private int amount;
    @JsonIgnore
    private int currentAmount;
    @JsonIgnore
    private License license;

    private int posX;
    private int posY;
    private int depth;

    public Dig() {
    }

    public Dig(int amount, int currentAmount, License license, int posX, int posY, int depth) {
        this.amount = amount;
        this.currentAmount = currentAmount;
        this.license = license;
        this.posX = posX;
        this.posY = posY;
        this.depth = depth;
    }

    @JsonIgnore
    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @JsonIgnore
    public int getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(int currentAmount) {
        this.currentAmount = currentAmount;
    }

    @JsonIgnore
    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public int getLicenseID() {
        return license.getId();
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        return "DigFull{" +
                "amount=" + amount +
                ", currentAmount=" + currentAmount +
                ", license=" + license +
                ", licenseID=" + license.getId() +
                ", posX=" + posX +
                ", posY=" + posY +
                ", depth=" + depth +
                '}';
    }
}
