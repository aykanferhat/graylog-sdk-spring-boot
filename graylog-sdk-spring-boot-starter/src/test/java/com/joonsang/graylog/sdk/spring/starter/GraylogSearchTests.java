package com.joonsang.graylog.sdk.spring.starter;

import com.joonsang.graylog.sdk.spring.starter.autoconfigure.GraylogSdkAutoConfiguration;
import com.joonsang.graylog.sdk.spring.starter.constant.*;
import com.joonsang.graylog.sdk.spring.starter.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = {
        GraylogSdkAutoConfiguration.class
    },
    properties = {
        "spring.main.banner-mode=off"
    }
)
public class GraylogSearchTests {

    @Value("${graylog.streamId}")
    String GRAYLOG_STREAM_ID;

    @Autowired
    GraylogSearch graylogSearch;

    @Test
    void messages() throws IOException {
        Timerange timerange = Timerange.builder().type(TimeRangeType.relative).range(300).build();
        SortConfig sort = SortConfig.builder().field("timestamp").order(SortConfigOrder.DESC).build();

        @SuppressWarnings("unchecked")
        Page<TestMessage> messages = (Page<TestMessage>) graylogSearch.getMessages(
            List.of(GRAYLOG_STREAM_ID),
            timerange,
            "message:API_REQUEST_FINISHED",
            10,
            1,
            sort,
            TestMessage.class
        );

        assertThat(messages).isNotNull();
    }

    @Test
    void statistics() throws IOException {
        Timerange timerange = Timerange.builder().type(TimeRangeType.relative).range(300).build();

        List<Series> seriesList = List.of(
            Series.builder().type(SeriesType.avg).field("process_time").build(),
            Series.builder().type(SeriesType.count).field("process_time").build(),
            Series.builder().type(SeriesType.min).field("process_time").build(),
            Series.builder().type(SeriesType.max).field("process_time").build(),
            Series.builder().type(SeriesType.percentile).percentile(95.0f).field("process_time").build(),
            Series.builder().type(SeriesType.percentile).percentile(99.0f).field("process_time").build(),
            Series.builder().type(SeriesType.count).build(),
            Series.builder().type(SeriesType.card).field("source").build()
        );

        List<Statistics> statistics = graylogSearch.getStatistics(
            List.of(GRAYLOG_STREAM_ID),
            timerange,
            "message:API_REQUEST_FINISHED",
            seriesList
        );

        assertThat(statistics).isNotNull();
    }

    @Test
    void terms() throws IOException {
        Timerange timerange = Timerange.builder().type(TimeRangeType.relative).range(300).build();

        SortConfig sort = SortConfig.builder()
            .type(SortConfigType.series)
            .field("count()")
            .direction(SortConfigDirection.Descending)
            .build();

        List<Series> seriesList = List.of(
            Series.builder().type(SeriesType.count).build(),
            Series.builder().type(SeriesType.avg).field("process_time").build()
        );

        List<SearchTypePivot> rowGroups = List.of(
            SearchTypePivot.builder().type(SearchTypePivotType.values).field("client_id").limit(10).build(),
            SearchTypePivot.builder().type(SearchTypePivotType.values).field("client_name").limit(10).build()
        );

        List<SearchTypePivot> columnGroups = List.of(
            SearchTypePivot.builder().type(SearchTypePivotType.values).field("grant_type").limit(5).build(),
            SearchTypePivot.builder().type(SearchTypePivotType.values).field("token_policy").limit(5).build()
        );

        Terms terms = graylogSearch.getTerms(
            List.of(GRAYLOG_STREAM_ID),
            timerange,
            "message:API_REQUEST_FINISHED",
            seriesList,
            rowGroups,
            columnGroups,
            sort
        );

        assertThat(terms).isNotNull();
    }

    @Test
    void histogram() throws IOException {
        Timerange timerange = Timerange.builder().type(TimeRangeType.relative).range(300).build();

        Interval interval = Interval.builder()
            .type(IntervalType.timeunit)
            .timeunit(IntervalTimeunit.get(IntervalTimeunit.Unit.minutes, 1))
            .build();

        List<Series> seriesList = List.of(
            Series.builder().type(SeriesType.count).build(),
            Series.builder().type(SeriesType.avg).field("process_time").build()
        );

        List<SearchTypePivot> columnGroups = List.of();

        Histogram histogram = graylogSearch.getHistogram(
            List.of(GRAYLOG_STREAM_ID),
            timerange,
            interval,
            "message:API_REQUEST_FINISHED",
            seriesList,
            columnGroups
        );

        assertThat(histogram).isNotNull();
    }

    @Test
    void raw() throws IOException {
        List<String> streamIds = List.of(GRAYLOG_STREAM_ID);

        List<SearchFilter> filters = streamIds.stream()
            .map(streamId -> SearchFilter.builder().id(streamId).build())
            .collect(Collectors.toList());

        SearchSpec searchSpec = SearchSpec.builder()
            .query(
                Query.builder()
                    .filter(Filter.builder().filters(filters).build())
                    .query(SearchQuery.builder().queryString("message:API_REQUEST_FINISHED").build())
                    .timerange(Timerange.builder().type(TimeRangeType.relative).range(300).build())
                    .searchType(
                        SearchType.builder()
                            .name("chart")
                            .series(List.of(Series.builder().type(SeriesType.count).build()))
                            .rollup(true)
                            .rowGroups(
                                List.of(
                                    SearchTypePivot.builder()
                                        .type(SearchTypePivotType.values)
                                        .field("client_name")
                                        .limit(15)
                                        .build()
                                )
                            )
                            .columnGroups(List.of())
                            .sort(List.of())
                            .type(SearchTypeType.pivot)
                            .build()
                    )
                    .build()
            )
            .build();

        String result = graylogSearch.raw(searchSpec);

        assertThat(result).isNotNull();
    }
}
