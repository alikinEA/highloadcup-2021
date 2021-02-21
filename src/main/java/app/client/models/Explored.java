package app.client.models;

public class Explored {
    private Area area;
    private int amount;

    public Explored() {

    }

    public Explored(Area area, int amount) {
        this.area = area;
        this.amount = amount;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Explored{" +
                "area=" + area +
                ", amount=" + amount +
                '}';
    }
}
