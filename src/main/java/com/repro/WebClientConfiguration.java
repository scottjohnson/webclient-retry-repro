package com.repro;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 *
 */
@Configuration
public class WebClientConfiguration {

    // Uncomment to use Jetty client connector
    @Bean
    @Primary
    public WebClient getJettyWebClient() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        HttpClient client = new HttpClient(sslContextFactory);
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(client)).build();
    }

    // Uncomment to use Reactor-Netty client connector
//    @Bean
//    @Primary
//    public WebClient getNettyWebClient() {
//        return WebClient.builder().clientConnector(new ReactorClientHttpConnector()).build();
//    }
}
