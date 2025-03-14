package org.opensource.proxy.api;

import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.opensource.proxy.RouterServer;
import org.opensource.proxy.config.RouterConfig;
import org.opensource.proxy.repository.RouterConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Service
@RestController
@RequestMapping("/api")
public class RouterApiService {

    private final RouterConfigRepository repository;
    public final Map<RouterConfig, RouterServer> routerServerMap;

    @Autowired
    public RouterApiService(RouterConfigRepository repository, Map<RouterConfig, RouterServer> routerServerMap) {
        this.repository = repository;
        this.routerServerMap = routerServerMap;
    }

    @Operation(summary = "Add a new routing rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Routing rule added successfully",
                    content = { @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = { @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) })
    })
    @PostMapping("/add")
    public ResponseEntity<String> addRoutingRule(@Parameter(description = "Routing rule details in CSV format: routingName,enterPort,routingDestination,routingPort,description") @RequestBody String body) {
        String[] parts = body.split(",");
        if (parts.length != 5) {
            return new ResponseEntity<>("Bad request", HttpStatus.BAD_REQUEST);
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

        return new ResponseEntity<>("Routing rule added successfully", HttpStatus.OK);
    }

    @Operation(summary = "Delete a routing rule")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Routing rule deleted successfully",
                    content = { @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = { @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) })
    })
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteRoutingRule(@Parameter(description = "Routing name to delete") @RequestBody String body) throws Exception {
        String routingName = body.trim();

        RouterConfig config = repository.read(routingName);
        if (config == null) {
            return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
        }

        repository.delete(routingName);
        RouterServer routerServer = routerServerMap.remove(config);
        if (routerServer != null) {
            routerServer.shutdown();
        }

        return new ResponseEntity<>("Routing rule deleted successfully", HttpStatus.OK);
    }

    @Operation(summary = "List all routing rules in JSON format")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of routing rules",
                    content = { @Content(mediaType = "application/json", schema = @Schema(implementation = RouterConfig.class)) })
    })
    @GetMapping("/list/json")
    public ResponseEntity<String> listRoutingRulesJson() {
        List<RouterConfig> configs = repository.findAll();
        String json = new Gson().toJson(configs);
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    @Operation(summary = "List all routing rules in CSV format")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of routing rules",
                    content = { @Content(mediaType = "text/csv", schema = @Schema(implementation = String.class)) }),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = { @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class)) })
    })
    @GetMapping("/list/csv")
    public ResponseEntity<String> listRoutingRulesCsv() {
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
            // Ensure the CSVPrinter flushes the content
            csvPrinter.flush();
            String csvContent = writer.toString();
            return new ResponseEntity<>(csvContent, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
