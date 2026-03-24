package com.example.movienotifier.config;

import java.net.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(RestClientConfig.class);

    /**
     * Creates the shared RestClient used to call external HTTP APIs.
     *
     * @return configured RestClient instance with redirect handling and logging interceptor
     */
    @Bean
    public RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        return RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .requestInterceptor((request, body, execution) -> {
                logger.info("Outbound HTTP request: {} {}", request.getMethod(), request.getURI());
                var response = execution.execute(request, body);
                logger.info(
                    "Outbound HTTP response: status={} contentType={} contentLength={}",
                    response.getStatusCode(),
                    response.getHeaders().getContentType(),
                    response.getHeaders().getContentLength()
                );
                return response;
            })
            .build();
    }
}
