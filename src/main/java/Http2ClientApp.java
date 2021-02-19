import http2.Http2ClientInitializer;
import http2.Http2ClientResponseHandler;
import http2.Http2SettingsHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import http2.utils.Http2Util;

import java.util.concurrent.TimeUnit;

public class Http2ClientApp {

    private static final int TIMEOUT = 2;
    private static final String HOST = "64.233.163.105";
    private static final int PORT = 443;
    private static Channel channel;

    /*public static void main(String[] args) throws Exception {

        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Http2ClientInitializer initializer = new Http2ClientInitializer(Http2Util.createSSLContext(false), Integer.MAX_VALUE, HOST, PORT);

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.remoteAddress(HOST, PORT);
            b.handler(initializer);

            channel = b.connect()
                    .syncUninterruptibly()
                    .channel();

            System.out.println("Connected to [" + HOST + ':' + PORT + ']');

            Http2SettingsHandler http2SettingsHandler = initializer.getSettingsHandler();
            http2SettingsHandler.awaitSettings(TIMEOUT, TimeUnit.SECONDS);

            System.out.println("Sending request(s)...");

            FullHttpRequest request = Http2Util.createGetRequest(HOST, PORT);

            Http2ClientResponseHandler responseHandler = initializer.getResponseHandler();
            int streamId = 3;

            responseHandler.put(streamId, channel.write(request), channel.newPromise());
            channel.flush();
            String response = responseHandler.awaitResponses(TIMEOUT, TimeUnit.SECONDS);

            System.out.println(response);

            System.out.println("Finished HTTP/2 request(s)");

        } finally {
            workerGroup.shutdownGracefully();
        }
    }*/
}
