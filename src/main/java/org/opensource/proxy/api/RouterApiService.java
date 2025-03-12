package org.opensource.proxy.api;

import com.google.gson.Gson;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.opensource.proxy.RouterServer;
import org.opensource.proxy.config.RouterConfig;
import org.opensource.proxy.repository.RouterConfigRepository;

import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class RouterApiService {

    private final int port;
    private final RouterConfigRepository repository;
    private final Map<RouterConfig, RouterServer> routerServerMap;

    public RouterApiService(int port, RouterConfigRepository repository, Map<RouterConfig, RouterServer> routerServerMap) {
        this.port = port;
        this.repository = repository;
        this.routerServerMap = routerServerMap;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(new HttpServerCodec());
                     p.addLast(new HttpObjectAggregator(1048576));
                     p.addLast(new ApiServiceHandler(repository, routerServerMap));
                 }
             });

            Channel ch = b.bind(port).sync().channel();
            System.out.println("API service started at port " + port);
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class ApiServiceHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final RouterConfigRepository repository;
        private final Map<RouterConfig, RouterServer> routerServerMap;

        public ApiServiceHandler(RouterConfigRepository repository, Map<RouterConfig, RouterServer> routerServerMap) {
            this.repository = repository;
            this.routerServerMap = routerServerMap;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            if (request.method() == HttpMethod.POST && request.uri().equals("/add")) {
                handleAddRequest(ctx, request);
            } else if (request.method() == HttpMethod.DELETE && request.uri().equals("/delete")) {
                handleDeleteRequest(ctx, request);
            } else if (request.method() == HttpMethod.GET && request.uri().equals("/list/json")) {
                handleListJsonRequest(ctx);
            } else if (request.method() == HttpMethod.GET && request.uri().equals("/list/csv")) {
                handleListCsvRequest(ctx);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
            }
        }

        private void handleAddRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
            String body = request.content().toString(StandardCharsets.UTF_8);
            String[] parts = body.split(",");
            if (parts.length != 5) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            String routingName = parts[0].trim();
            int enterPort = Integer.parseInt(parts[1].trim());
            String routingDestination = parts[2].trim();
            int routingPort = Integer.parseInt(parts[3].trim());
            String description = parts[4].trim();

            RouterConfig config = new RouterConfig(routingName, enterPort, routingDestination, routingPort, description);
            repository.create(config);

            RouterServer routerServer = new RouterServer(config.getEnterPort(), config.getRoutingDestination(), config.getRoutingPort());
            routerServer.runDaemon();
            routerServerMap.put(config, routerServer);

            sendSuccessResponse(ctx, "Routing rule added successfully");
        }

        private void handleDeleteRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
            String body = request.content().toString(StandardCharsets.UTF_8);
            String routingName = body.trim();

            RouterConfig config = repository.read(routingName);
            if (config == null) {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            repository.delete(routingName);
            RouterServer routerServer = routerServerMap.remove(config);
            if (routerServer != null) {
                routerServer.shutdown();
            }

            sendSuccessResponse(ctx, "Routing rule deleted successfully");
        }

        private void handleListJsonRequest(ChannelHandlerContext ctx) {
            List<RouterConfig> configs = repository.findAll();
            String json = new Gson().toJson(configs);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer().writeBytes(json.getBytes(StandardCharsets.UTF_8)));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private void handleListCsvRequest(ChannelHandlerContext ctx) {
            List<RouterConfig> configs = repository.findAll();

            try (Writer writer = new StringWriter();
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("routingName", "enterPort", "routingDestination", "routingPort", "description"))) {

                for (RouterConfig config : configs) {
                    csvPrinter.printRecord(
                            config.getRoutingName(),
                            config.getEnterPort(),
                            config.getRoutingDestination(),
                            config.getRoutingPort(),
                            config.getDescription()
                    );
                }

                String csvContent = writer.toString();
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        ctx.alloc().buffer().writeBytes(csvContent.getBytes(StandardCharsets.UTF_8))
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/csv");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception e) {
                sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }

        private void sendSuccessResponse(ChannelHandlerContext ctx, String message) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ctx.alloc().buffer());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            response.content().writeBytes(message.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);;
        }

        private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, ctx.alloc().buffer());
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);;
        }
    }
}