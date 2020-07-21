package com.joonsang.graylog.sdk.spring.starter.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Builder
@Getter
public class Filter {

    @Builder.Default
    private final String type = "or";

    @Singular
    private final List<SearchFilter> filters;
}
