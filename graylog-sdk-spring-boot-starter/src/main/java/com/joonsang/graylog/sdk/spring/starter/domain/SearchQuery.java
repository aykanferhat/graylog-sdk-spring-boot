package com.joonsang.graylog.sdk.spring.starter.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SearchQuery {

    @Builder.Default
    private final String type = "elasticsearch";

    @Builder.Default
    @JsonProperty("query_string")
    private final String queryString = "";
}
