package org.opensource.proxy;

import org.opensource.proxy.config.RouterConfig;
import org.opensource.proxy.repository.CSVRouterConfigRepository;
import org.opensource.proxy.repository.RouterConfigRepository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyApplication {

    public static Set<RouterConfig> configSet = ConcurrentHashMap.newKeySet();
    public static void main(String[] args) throws Exception {
        String configFilePath = "config.csv"; // Default config file path

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--config") && i + 1 < args.length) {
                configFilePath = args[i + 1];
                break;
            }
        }

        RouterConfigRepository repository = new CSVRouterConfigRepository(configFilePath);
        List<RouterConfig> routerConfigs = repository.findAll();
        configSet.addAll(routerConfigs);

        for (RouterConfig config : configSet) {
            new RouterServer(config.getEnterPort(), config.getRoutingDestination(), config.getRoutingPort()).runDaemon();
        }
    }
}