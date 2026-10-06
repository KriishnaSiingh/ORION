package io.orion.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.neo4j.Neo4jRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(
    scanBasePackages = "io.orion",
    exclude = {
        Neo4jDataAutoConfiguration.class,
        Neo4jRepositoriesAutoConfiguration.class,
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        FlywayAutoConfiguration.class
    }
)
@EnableJpaRepositories(basePackages = "io.orion")
@EntityScan(basePackages = "io.orion")
public class OrionApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrionApplication.class, args);
    }
}
