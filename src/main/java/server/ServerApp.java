package server;

import app.client.models.License;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import io.javalin.Javalin;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerApp {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Javalin app = Javalin.create().start(8000);
        app.get("/heal-check", ctx -> {
            ctx.result("Ok");
            System.out.println(atomicInteger.incrementAndGet());
           // System.out.println(threadDump(false, false));
        });
        app.get("/licenses", ctx -> {
            ctx.result("license1");
            System.out.println(atomicInteger.incrementAndGet());
            // System.out.println(threadDump(false, false));
        });

        app.post("/licenses", ctx -> {
            ctx.result(JsonStream.serialize(new License(1,1,1)));
            System.out.println(atomicInteger.incrementAndGet());
            // System.out.println(threadDump(false, false));
        });
    }

    private static String threadDump(boolean lockedMonitors, boolean lockedSynchronizers) {
        StringBuffer threadDump = new StringBuffer(System.lineSeparator());
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for(ThreadInfo threadInfo : threadMXBean.dumpAllThreads(lockedMonitors, lockedSynchronizers)) {
            threadDump.append(threadInfo.toString());
        }
        return threadDump.toString();
    }
}
