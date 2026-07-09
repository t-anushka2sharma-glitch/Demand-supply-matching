package com.marketmaker.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java Exercise 1 - Part 3/4 [Spring]: Web backend application.
 *
 * Boots a REST API exposing supply/demand order submission and order-status
 * lookup, backed by the same {@link com.marketmaker.engine.MatchingEngine}
 * and plain-JDBC persistence used by Parts 1 & 2.
 *
 * Run with: mvn spring-boot:run
 * Or:       java -jar target/demand-supply-matching.jar
 */
@SpringBootApplication
public class DemandSupplyApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemandSupplyApplication.class, args);
    }
}
