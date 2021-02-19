package server;

import io.javalin.Javalin;

import java.util.concurrent.atomic.AtomicInteger;

public class ServerApp {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx -> {
            ctx.result("Hello World");
            System.out.println(atomicInteger.incrementAndGet());
        });
    }
}
