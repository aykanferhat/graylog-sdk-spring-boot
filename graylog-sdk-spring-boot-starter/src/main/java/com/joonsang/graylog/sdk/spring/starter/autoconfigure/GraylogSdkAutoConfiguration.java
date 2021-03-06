package com.joonsang.graylog.sdk.spring.starter.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.joonsang.graylog.sdk.spring.starter.LegacyGraylogSearch;
import com.joonsang.graylog.sdk.spring.starter.GraylogRequest;
import com.joonsang.graylog.sdk.spring.starter.GraylogSearch;
import com.joonsang.graylog.sdk.spring.starter.search.LegacySearchAbsolute;
import com.joonsang.graylog.sdk.spring.starter.search.Search;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Graylog SDK Auto Configuration
 * @author debugrammer
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(
    {
        LegacyGraylogSdkProperties.class,
        GraylogApiProperties.class
    }
)
public class GraylogSdkAutoConfiguration {

    private final LegacyGraylogSdkProperties legacyGraylogSdkProperties;

    private final GraylogApiProperties graylogApiProperties;

    public GraylogSdkAutoConfiguration(
        LegacyGraylogSdkProperties legacyGraylogSdkProperties,
        GraylogApiProperties graylogApiProperties
    ) {

        this.legacyGraylogSdkProperties = legacyGraylogSdkProperties;
        this.graylogApiProperties = graylogApiProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "graylogObjectMapper")
    public ObjectMapper graylogObjectMapper() {
        return Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .modules(new JavaTimeModule())
            .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "graylogOkHttpClient")
    public OkHttpClient graylogOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(10, 10, TimeUnit.SECONDS))
            .retryOnConnectionFailure(true)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS);

        builder.networkInterceptors().add(chain -> {
            Request request = chain.request().newBuilder()
                .addHeader("X-Requested-By", "XMLHttpRequest")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Authorization", "Basic " + graylogApiProperties.getCredentials())
                .addHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

            return chain.proceed(request);
        });

        return builder.build();
    }

    @Bean
    @ConditionalOnBean(name = {"graylogObjectMapper", "graylogOkHttpClient"})
    @ConditionalOnMissingBean(name = "legacyGraylogSearch")
    public LegacyGraylogSearch legacyGraylogSearch(
        @Qualifier("graylogObjectMapper") ObjectMapper objectMapper,
        @Qualifier("graylogOkHttpClient") OkHttpClient okHttpClient
    ) {

        GraylogRequest request = new GraylogRequest(okHttpClient, graylogApiProperties);
        LegacySearchAbsolute absolute = new LegacySearchAbsolute(request, legacyGraylogSdkProperties);

        return new LegacyGraylogSearch(objectMapper, absolute);
    }

    @Bean
    @ConditionalOnBean(name = {"graylogObjectMapper", "graylogOkHttpClient"})
    @ConditionalOnMissingBean(name = "graylogSearch")
    public GraylogSearch graylogSearch(
        @Qualifier("graylogObjectMapper") ObjectMapper objectMapper,
        @Qualifier("graylogOkHttpClient") OkHttpClient okHttpClient
    ) {

        GraylogRequest request = new GraylogRequest(okHttpClient, graylogApiProperties);
        Search search = new Search(request, graylogApiProperties, objectMapper);

        return new GraylogSearch(objectMapper, search);
    }
}
