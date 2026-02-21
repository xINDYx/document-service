package com.itq.generator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
public class GeneratorRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeneratorRunner.class);

    private final GeneratorProperties properties;
    private final RestTemplate restTemplate;

    public GeneratorRunner(GeneratorProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public void run(ApplicationArguments args) {
        int n = properties.getCount();
        String baseUrl = properties.getServiceUrl();
        String initiator = properties.getInitiator();

        log.info("Starting document generation: N={}, serviceUrl={}", n, baseUrl);

        int success = 0;
        int failed = 0;
        Instant start = Instant.now();

        for (int i = 1; i <= n; i++) {
            try {
                Map<String, String> body = Map.of(
                        "author", "Generator-Author-" + (i % 10 + 1),
                        "title", "Generated Document #" + i,
                        "initiator", initiator
                );
                restTemplate.postForObject(baseUrl + "/api/v1/documents", body, Map.class);
                success++;

                if (i % 100 == 0 || i == n) {
                    long elapsed = Duration.between(start, Instant.now()).toMillis();
                    log.info("Progress: {}/{} created ({} failed), elapsed={}ms", i, n, failed, elapsed);
                }
            } catch (Exception e) {
                failed++;
                log.warn("Failed to create document {}/{}: {}", i, n, e.getMessage());
            }
        }

        long total = Duration.between(start, Instant.now()).toMillis();
        log.info("Generation complete: total={}, success={}, failed={}, totalTime={}ms",
                n, success, failed, total);
    }
}
