package org.opensource;

import org.opensource.proxy.RouterServer;
import org.opensource.proxy.config.RouterConfig;
import org.opensource.proxy.repository.RouterConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class ProxyApplication implements CommandLineRunner {

    @Bean
    public static Map<RouterConfig, RouterServer> routerServerMap() {
        return new ConcurrentHashMap<>();
    }

    @Autowired
    public Map<RouterConfig, RouterServer> routerServerMap;
    @Autowired
    RouterConfigRepository repository;

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }

    public void run(String... args) throws Exception {
        Set<RouterConfig> configSet = ConcurrentHashMap.newKeySet();
        List<RouterConfig> routerConfigs = repository.findAll();
        configSet.addAll(routerConfigs);

        for (RouterConfig config : configSet) {
            RouterServer routerServer = new RouterServer(config.getEnterPort(), config.getRoutingDestination(), config.getRoutingPort());
            routerServer.runDaemon();
            routerServerMap.put(config, routerServer);
        }
    }
}