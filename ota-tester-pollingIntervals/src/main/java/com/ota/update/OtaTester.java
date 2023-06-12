package com.ota.update;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
@SpringBootApplication
public class OtaTester {

    public static void main(String[] args) {
        SpringApplication.run(OtaTester.class, args);
    }

}
