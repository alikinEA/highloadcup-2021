package app.client.models;

import java.net.http.HttpRequest;

public class ExploreFull {
    private HttpRequest httpRequest;
    private Area area;

    public ExploreFull() {

    }

    public ExploreFull(HttpRequest httpRequest, Area area) {
        this.httpRequest = httpRequest;
        this.area = area;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    @Override
    public String toString() {
        return "ExploreFull{" +
                "httpRequest=" + httpRequest +
                ", area=" + area +
                '}';
    }
}
