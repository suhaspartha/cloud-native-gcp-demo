package com.suhas.demo.config;

import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

@Configuration
@Profile("gcp")
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Bean
    public DataSource dataSource() throws IOException {
        log.info("Building DataSource — fetching credentials from Secret Manager");

        String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");

        String dbUser;
        String dbPassword;
        String connectionName;

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            dbUser         = fetchSecret(client, projectId, "db-username");
            dbPassword     = fetchSecret(client, projectId, "db-password");
            connectionName = fetchSecret(client, projectId, "db-connection-name");
        }

        String jdbcUrl = String.format(
                "jdbc:postgresql:///%s" +
                        "?cloudSqlInstance=%s" +
                        "&socketFactory=com.google.cloud.sql.postgres.SocketFactory" +
                        "&ipTypes=PUBLIC",
                "orders_db",
                connectionName
        );

        log.info("Connecting to Cloud SQL instance: {}", connectionName);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setPoolName("orders-pool");

        return new HikariDataSource(config);
    }

    private String fetchSecret(SecretManagerServiceClient client,
                               String projectId, String secretId) {
        SecretVersionName name = SecretVersionName.of(projectId, secretId, "latest");
        return client.accessSecretVersion(name)
                .getPayload()
                .getData()
                .toStringUtf8();
    }
}