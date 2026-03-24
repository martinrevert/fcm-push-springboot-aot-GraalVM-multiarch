package ar.com.martinrevert.movienotifier.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(DataSourceConfig.DataSourceProperties.class)
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    /**
     * Builds the primary datasource using MariaDB datasource properties.
     *
     * @param props bound datasource properties
     * @return configured Hikari datasource
     */
    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties props) {
        logger.info("Initializing DataSource explicitly.");
        logger.info("URL: {}", props.getUrl());
        logger.info("Username: {}", props.getUsername());
        logger.info("Password: {}", (props.getPassword() != null && !props.getPassword().isEmpty()) ? "****" : "NOT SET");
        logger.info("Driver Class: {}", props.getDriverClassName());

        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.mariadb.jdbc.MariaDbDataSource");
        Properties dataSourceProps = new Properties();
        if (props.getUrl() != null) {
            dataSourceProps.setProperty("url", props.getUrl());
        }
        if (props.getUsername() != null) {
            dataSourceProps.setProperty("user", props.getUsername());
        }
        if (props.getPassword() != null) {
            dataSourceProps.setProperty("password", props.getPassword());
        }
        config.setDataSourceProperties(dataSourceProps);
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    @ConfigurationProperties(prefix = "spring.datasource")
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        /**
         * Gets the JDBC URL.
         *
         * @return datasource URL
         */
        public String getUrl() { return url; }

        /**
         * Sets the JDBC URL.
         *
         * @param url datasource URL
         */
        public void setUrl(String url) { this.url = url; }

        /**
         * Gets the datasource username.
         *
         * @return datasource username
         */
        public String getUsername() { return username; }

        /**
         * Sets the datasource username.
         *
         * @param username datasource username
         */
        public void setUsername(String username) { this.username = username; }

        /**
         * Gets the datasource password.
         *
         * @return datasource password
         */
        public String getPassword() { return password; }

        /**
         * Sets the datasource password.
         *
         * @param password datasource password
         */
        public void setPassword(String password) { this.password = password; }

        /**
         * Gets the JDBC driver class name.
         *
         * @return driver class name
         */
        public String getDriverClassName() { return driverClassName; }

        /**
         * Sets the JDBC driver class name.
         *
         * @param driverClassName driver class name
         */
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    }
}

