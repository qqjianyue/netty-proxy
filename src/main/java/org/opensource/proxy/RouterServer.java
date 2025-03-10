package org.opensource.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class RouterServer {

    private static final Logger logger = LoggerFactory.getLogger(RouterServer.class);

    private final int port;
    private final String destination;
    private final int destinationPort;

    ChannelFuture f;
    EventLoopGroup bossGroup;
    EventLoopGroup workerGroup;

    private ScheduledExecutorService executorService;

    public RouterServer(int port, String destination, int destinationPort) {
        this.port = port;
        this.destination = destination;
        this.destinationPort = destinationPort;
    }

    public void run() throws Exception {
        bossGroup = new NioEventLoopGroup(); // (1)
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class) // (3)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new RouterClientHandler(destination, destinationPort));
                 }
             });

            // Bind and start to accept incoming connections.
            f = b.bind(port).sync(); // (5)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
            logger.info("closed");
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void shutdown() throws Exception {
        // Add logging to indicate when the server is shutting down
        logger.info("Shutting down RouterServer...");

        // Wait until the server socket is closed.
        if (f != null && f.channel() != null) {
            f.channel().close();
        } else {
            logger.info("Server socket was already closed or never opened.");
        }

        // Shut down all event loops to terminate all threads.
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        } else {
            logger.info("WorkerGroup was already shut down or never initialized.");
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        } else {
            logger.info("BossGroup was already shut down or never initialized.");
        }

        // Shutdown the executor service
        if (executorService != null) {
            executorService.shutdown();
        }

        logger.info("RouterServer shut down complete.");
    }

    public void runDaemon() {
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.execute(() -> {
            try {
                run();
            } catch (Exception e) {
                logger.error("Error running RouterServer", e);
            }
        });
    }
}
