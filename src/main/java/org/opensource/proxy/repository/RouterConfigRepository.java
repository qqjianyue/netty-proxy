package org.opensource.proxy.repository;

import org.opensource.proxy.config.RouterConfig;

import java.util.List;

public interface RouterConfigRepository {
    void create(RouterConfig config);
    RouterConfig read(String routingName);
    void update(RouterConfig config);
    void delete(String routingName);
    List<RouterConfig> findAll();
}