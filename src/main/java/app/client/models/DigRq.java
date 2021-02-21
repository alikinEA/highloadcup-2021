package app.client.models;

public class DigRq {
    public DigRq() {

    }

    public DigRq(int licenseID, int posX, int posY, int depth) {
        this.licenseID = licenseID;
        this.posX = posX;
        this.posY = posY;
        this.depth = depth;
    }

    private int licenseID;
    private int posX;
    private int posY;
    private int depth;

    public int getLicenseID() {
        return licenseID;
    }

    public void setLicenseID(int licenseID) {
        this.licenseID = licenseID;
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
}
