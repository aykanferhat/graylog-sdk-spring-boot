package com.joonsang.graylog.sdk.spring.starter.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.joonsang.graylog.sdk.spring.starter.constant.SearchTypePivotType;
import lombok.Builder;
import lombok.Getter;

/**
 * Search Type Pivot
 * @author debugrammer
 * @since 2.0.0
 */
@Builder
@Getter
public class SearchTypePivot {

    private final SearchTypePivotType type;

    private final String field;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Integer limit;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Interval interval;
}
