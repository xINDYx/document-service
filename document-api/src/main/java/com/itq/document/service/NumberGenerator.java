package com.itq.document.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class NumberGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicLong counter = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    public String generate() {
        String date = LocalDate.now().format(FMT);
        long seq = counter.incrementAndGet();
        return "DOC-%s-%06d".formatted(date, seq % 1_000_000);
    }
}
