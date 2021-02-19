import http1.handlers.TimeoutHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class App {

    public Bootstrap client(EventLoopGroup workerGroup) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Bootstrap b = new Bootstrap();
        b.group(workerGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //HttpClient codec is a helper ChildHandler that encompasses
                        //both HTTP response decoding and HTTP request encoding
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new ReadTimeoutHandler(1));
                        //HttpObjectAggregator helps collect chunked HttpRequest pieces into
                        //a single FullHttpRequest. If you don't make use of streaming, this is
                        //much simpler to work with.
                        ch.pipeline().addLast(new HttpObjectAggregator(1048576));
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
                                final String echo = msg.content().toString(CharsetUtil.UTF_8);
                                System.out.println("Response: {}" + echo + " thread = " + Thread.currentThread().toString() + " count =" + atomicInteger.incrementAndGet());
                            }
                        });
                    }
                });
        return b;
    }

    public static void main(String[] args) throws Exception {
        //I find while learning Netty to keep resource leak detecting at Paranoid,
        //however *DO NOT* ship with this level.
        //ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        App app = new App();
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap clientBootstrap = app.client(workerGroup);
           // final ByteBuf content = Unpooled.copiedBuffer("Hello World!", CharsetUtil.UTF_8);

            var f = clientBootstrap
                    .connect("localhost", 7000);
            HttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, "/", Unpooled.EMPTY_BUFFER);
            request.headers().set(HttpHeaderNames.HOST, "localhost");
            request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

            for (int i = 0; i < 10_000 ; i++) {
                f.addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                           /* // Prepare the HTTP request.
                            HttpRequest request = new DefaultFullHttpRequest(
                                    HttpVersion.HTTP_1_1, HttpMethod.POST, "/", content);
                            // If we don't set a content length from the client, HTTP RFC
                            // dictates that the body must be be empty then and Netty won't read it.
                            request.headers().set("Content-Length", content.readableBytes());
                            future.channel().writeAndFlush(request);*/
                                future.channel().writeAndFlush(request);
                            }
                        });
            }
            while (true) {

            }
        } finally {
            //Gracefully shutdown both event loop group
            workerGroup.shutdownGracefully();
        }
    }


}