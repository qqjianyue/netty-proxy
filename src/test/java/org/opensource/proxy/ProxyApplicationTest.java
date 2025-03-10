package org.opensource.proxy;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

public class ProxyApplicationTest {

    private static TestServer[] testServers = new TestServer[3];
    private static File tempCsvFile;
    private static Thread proxyThread;

    @BeforeClass
    public static void setup() throws Exception {
        // 1. Setup test HTTP endpoints
        for (int i = 0; i < 3; i++) {
            testServers[i] = new TestServer(15001 + i);
        }

        // 2. Create CSV configuration file
        tempCsvFile = File.createTempFile("test-proxy-", ".csv");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(tempCsvFile), CSVFormat.DEFAULT.withHeader("RoutingName", "EnterPort", "RoutingDestination", "RoutingPort", "Description"))) {
            printer.printRecord("rule1", 25001, "localhost", 15001, "Test Rule 1");
            printer.printRecord("rule2", 25002, "localhost", 15002, "Test Rule 2");
            printer.printRecord("rule3", 25003, "localhost", 15003, "Test Rule 3");
        }

        // 3. Initialize ProxyApplication in separate thread
        proxyThread = new Thread(() -> {
            try {
                ProxyApplication.main(new String[]{"--config", tempCsvFile.getAbsolutePath()});
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();

        // Wait for servers to start
        Thread.sleep(5000);
    }

    @Test
    public void testProxyFunctionality() throws Exception {
        for (int i = 0; i < 3; i++) {
            int proxyPort = 25001 + i;
            String expectedResponse = "test" + (i + 1);
            String result = sendRequest("http://localhost:" + proxyPort);
            Assert.assertEquals(expectedResponse, result);
        }
    }

    @AfterClass
    public static void teardown() {
        // Gracefully shutdown servers
        for (TestServer server : testServers) {
            if (server != null) server.stop();
        }
        tempCsvFile.delete();
    }

    private static String sendRequest(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.readLine();
        }
    }

    // Simple embedded test server implementation
    private static class TestServer {
        private final int port;
        private HttpServer server;

        public TestServer(int port) throws IOException {
            this.port = port;
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/").setHandler(exchange -> {
                String response = "test" + (port - 15000);
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
        }

        public void stop() {
            if (server != null) server.stop(0);
        }
    }
}
