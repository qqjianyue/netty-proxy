package org.opensource.proxy.repository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.opensource.proxy.config.ApplicationConfig;
import org.opensource.proxy.config.RouterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CSVRouterConfigRepository implements RouterConfigRepository, InitializingBean {
    @Autowired
    ApplicationConfig applicationConfig;

    private final Set<RouterConfig> cacheSet = ConcurrentHashMap.newKeySet();

    private static final Logger logger = LoggerFactory.getLogger(CSVRouterConfigRepository.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        cacheSet.addAll(findAll());
    }

    @Override
    public void create(RouterConfig config) {
        if (cacheSet.contains(config)) {
            throw new IllegalArgumentException("The combination of enter port, routing destination, and routing port must be unique.");
        }
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(applicationConfig.getFilePath(), true), CSVFormat.DEFAULT.withHeader("RoutingName", "EnterPort", "RoutingDestination", "RoutingPort", "Description"))) {
            printer.printRecord(config.getRoutingName(), config.getEnterPort(), config.getRoutingDestination(), config.getRoutingPort(), config.getDescription());
            cacheSet.add(config);
        } catch (IOException e) {
            logger.error("Error creating RouterConfig in CSV file", e);
        }
    }

    @Override
    public RouterConfig read(String routingName) {
        try (CSVParser parser = new CSVParser(new FileReader(applicationConfig.getFilePath()), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                if (record.get("RoutingName").equals(routingName)) {
                    return new RouterConfig(
                            record.get("RoutingName"),
                            Integer.parseInt(record.get("EnterPort")),
                            record.get("RoutingDestination"),
                            Integer.parseInt(record.get("RoutingPort")),
                            record.get("Description")
                    );
                }
            }
        } catch (IOException e) {
            logger.error("Error reading RouterConfig from CSV file", e);
        }
        return null;
    }

    @Override
    public void update(RouterConfig config) {
        if (cacheSet.contains(config)) {
            throw new IllegalArgumentException("The combination of enter port, routing destination, and routing port must be unique.");
        }
        List<RouterConfig> configs = findAll();
        configs.removeIf(c -> c.getRoutingName().equals(config.getRoutingName()));
        configs.add(config);
        saveAll(configs);

        cacheSet.remove(config);
        cacheSet.add(config);

    }

    @Override
    public void delete(String routingName) {
        List<RouterConfig> configs = findAll();
        configs.removeIf(c -> c.getRoutingName().equals(routingName));
        saveAll(configs);

        cacheSet.removeIf(c -> c.getRoutingName().equals(routingName));
    }

    @Override
    public List<RouterConfig> findAll() {
        if( !cacheSet.isEmpty() )
            return new ArrayList<>(cacheSet);
        List<RouterConfig> configs = new ArrayList<>();
        try (CSVParser parser = new CSVParser(new FileReader(applicationConfig.getFilePath()), CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : parser) {
                configs.add(new RouterConfig(
                        record.get("RoutingName"),
                        Integer.parseInt(record.get("EnterPort")),
                        record.get("RoutingDestination"),
                        Integer.parseInt(record.get("RoutingPort")),
                        record.get("Description")
                ));
            }
        } catch (IOException e) {
            logger.error("Error finding all RouterConfigs from CSV file", e);
        }
        return configs;
    }

    private void saveAll(List<RouterConfig> configs) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(applicationConfig.getFilePath()), CSVFormat.DEFAULT.withHeader("RoutingName", "EnterPort", "RoutingDestination", "RoutingPort", "Description"))) {
            for (RouterConfig config : configs) {
                printer.printRecord(config.getRoutingName(), config.getEnterPort(), config.getRoutingDestination(), config.getRoutingPort(), config.getDescription());
            }

            cacheSet.addAll(configs);
        } catch (IOException e) {
            logger.error("Error saving all RouterConfigs to CSV file", e);
        }
    }

}