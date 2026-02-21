package com.itq.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "document.workers")
public class WorkerProperties {

    private int batchSize = 100;
    private long submitIntervalMs = 10_000;
    private long approveIntervalMs = 15_000;

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public long getSubmitIntervalMs() { return submitIntervalMs; }
    public void setSubmitIntervalMs(long submitIntervalMs) { this.submitIntervalMs = submitIntervalMs; }

    public long getApproveIntervalMs() { return approveIntervalMs; }
    public void setApproveIntervalMs(long approveIntervalMs) { this.approveIntervalMs = approveIntervalMs; }
}
