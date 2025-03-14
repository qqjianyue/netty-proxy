package org.opensource;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensource.proxy.api.RouterApiService;
import org.opensource.proxy.config.ApplicationConfig;
import org.opensource.proxy.repository.RouterConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.Executors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

@SpringBootTest(classes = ProxyApplication.class)
@TestPropertySource(properties = {
        "router.config.path=" + ProxyApplicationTest.TEST_CSV
})
public class ProxyApplicationTest {

    private static final TestServer[] testServers = new TestServer[3];
    private static File tempCsvFile;
    public static final String TEST_CSV="./target/test-classes/test-proxy.csv";
    @Autowired
    private RouterConfigRepository repository;
    @Autowired
    ApplicationConfig applicationConfig;
    @Autowired
    RouterApiService routerApiService;

    @BeforeAll
    public static void setup() throws Exception {
        // Setup test HTTP endpoints
        for (int i = 0; i < 3; i++) {
            testServers[i] = new TestServer(15001 + i);
        }
        // Create CSV configuration file
        tempCsvFile = new File(ProxyApplicationTest.TEST_CSV);
        try (var printer = new CSVPrinter(new FileWriter(tempCsvFile), CSVFormat.Builder.create()
                .setHeader("RoutingName", "EnterPort", "RoutingDestination", "RoutingPort", "Description")
                .build())) {
            printer.printRecord("rule1", 25001, "localhost", 15001, "Test Rule 1");
            printer.printRecord("rule2", 25002, "localhost", 15002, "Test Rule 2");
            printer.printRecord("rule3", 25003, "localhost", 15003, "Test Rule 3");
        }
        // Wait for servers to start
        Thread.sleep(2000);
    }

    @Test
    public void testProxyFunctionality() throws Exception {
        for (int i = 0; i < 3; i++) {
            int proxyPort = 25001 + i;
            String expectedResponse = "test" + (i + 1);
            String result = sendRequest("http://localhost:" + proxyPort);
            Assertions.assertEquals(expectedResponse, result);
        }
        System.out.println("testProxyFunctionality complete");
    }

    @Test
    public void testProxyAPI() throws Exception {
        // Test adding a new routing rule
        String addRuleBody = "rule4,25004,localhost,15004,Test Rule 4";
        String addResponse = routerApiService.addRoutingRule(addRuleBody).getBody();
        Assertions.assertEquals("Routing rule added successfully", addResponse);

        // Test listing routing rules in JSON format
        String listJsonResponse = routerApiService.listRoutingRulesJson().getBody();
        assert listJsonResponse != null;
        Assertions.assertTrue(listJsonResponse.contains("rule4"));
        Assertions.assertTrue(listJsonResponse.contains("25004"));
        Assertions.assertTrue(listJsonResponse.contains("localhost"));
        Assertions.assertTrue(listJsonResponse.contains("15004"));
        Assertions.assertTrue(listJsonResponse.contains("Test Rule 4"));

        // Test listing routing rules in CSV format
        String listCsvResponse = routerApiService.listRoutingRulesCsv().getBody();
        assert listCsvResponse != null;
        Assertions.assertTrue(listCsvResponse.contains("rule4,25004,localhost,15004,Test Rule 4"));

        // Test deleting a routing rule
        String deleteRuleBody = "rule4";
        String deleteResponse = routerApiService.deleteRoutingRule(deleteRuleBody).getBody();
        Assertions.assertEquals("Routing rule deleted successfully", deleteResponse);

        // Verify the rule has been deleted by listing again
        String listJsonResponseAfterDelete = routerApiService.listRoutingRulesJson().getBody();
        assert listJsonResponseAfterDelete != null;
        Assertions.assertFalse(listJsonResponseAfterDelete.contains("rule4"));
    }

    private static String sendRequest(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.readLine();
        }
    }

    @AfterAll
    public static void teardown() {
        // Gracefully shutdown servers
        for (TestServer server : testServers) {
            if (server != null) server.stop();
        }
        tempCsvFile.delete();
    }

    // Simple embedded test server implementation
    private static class TestServer {
        private final HttpServer server;

        public TestServer(int port) throws IOException {
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