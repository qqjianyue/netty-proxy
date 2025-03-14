package org.opensource.proxy.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Data
@Component
public class ApplicationConfig {
    @Value("${router.config.path:./config.csv}")
    private String filePath;
    @Value("${api.port: 8081}")
    private Integer apiPort;

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> factory.setPort(apiPort); // Set your desired port number here
    }
}
