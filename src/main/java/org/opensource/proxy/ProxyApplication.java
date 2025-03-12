package org.opensource.proxy;

import org.opensource.proxy.api.RouterApiService;
import org.opensource.proxy.config.RouterConfig;
import org.opensource.proxy.repository.CSVRouterConfigRepository;
import org.opensource.proxy.repository.RouterConfigRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyApplication {

    public static Set<RouterConfig> configSet = ConcurrentHashMap.newKeySet();
    public static Map<RouterConfig, RouterServer> routerServerMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        String configFilePath = "config.csv"; // Default config file path
        int apiPort = 19999; // Default API port

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--config") && i + 1 < args.length) {
                configFilePath = args[i + 1];
                break;
            } else if (args[i].equals("--api-port") && i + 1 < args.length) {
                apiPort = Integer.parseInt(args[i + 1]);
                break;
            }
        }

        RouterConfigRepository repository = new CSVRouterConfigRepository(configFilePath);
        List<RouterConfig> routerConfigs = repository.findAll();
        configSet.addAll(routerConfigs);

        for (RouterConfig config : configSet) {
            RouterServer routerServer = new RouterServer(config.getEnterPort(), config.getRoutingDestination(), config.getRoutingPort());
            routerServer.runDaemon();
            routerServerMap.put(config, routerServer);
        }

        // Start API service
        RouterApiService routerApiService = new RouterApiService(apiPort, repository, routerServerMap);
        new Thread(() -> {
            try {
                routerApiService.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}