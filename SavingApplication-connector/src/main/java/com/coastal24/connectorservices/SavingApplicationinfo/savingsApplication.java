package com.coastal24.connectorservices.SavingApplicationinfo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.xtensifi.connectorservices.common.workflow.ConnectorConfig;
import com.xtensifi.connectorservices.common.workflow.ConnectorHubService;
import com.xtensifi.connectorservices.common.workflow.ConnectorHubServiceImpl;

@SpringBootApplication
public class savingsApplication {
    public static void main(String[] args) {
        SpringApplication.run(savingsApplication.class, args);
    }

    // @Bean
    // ConnectorHubService connectorHubService() {
    // return new ConnectorHubServiceImpl();
    // }

    // @Bean
    // ConnectorConfig connectorConfig() {
    // return new ConnectorConfig();
    // }
}