package org.example.gatewayservice.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration; // Added
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration // Added annotation here
public class RestClientConfig {

  @Value("${account-service.base-url:http://account-service:8081}")
  private String accountServiceBaseUrl;

  @Value("${maxTotal:100}")
  private int maxTotal;

  @Value("${maxPerRoute:20}")
  private int maxPerRoute;

  @Bean
  public RestClient restClient(RestClient.Builder builder) {

    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(maxTotal);
    cm.setDefaultMaxPerRoute(maxPerRoute);

    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(5))
            .setResponseTimeout(Timeout.ofSeconds(5))
            .build();

    CloseableHttpClient httpClient =
        HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(requestConfig)
            .build();

    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(httpClient);

    return builder
        .baseUrl(accountServiceBaseUrl)
        .requestFactory(factory)
        .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
        .build();
  }
}
