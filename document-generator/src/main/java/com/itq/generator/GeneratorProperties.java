package com.itq.generator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "generator")
public class GeneratorProperties {

    private int count = 100;
    private String serviceUrl = "http://localhost:8080";
    private String initiator = "generator-tool";

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }

    public String getInitiator() { return initiator; }
    public void setInitiator(String initiator) { this.initiator = initiator; }
}
