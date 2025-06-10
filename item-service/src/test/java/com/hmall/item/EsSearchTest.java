package com.hmall.item;


import com.hmall.common.domain.R;
import com.hmall.item.service.IItemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//指明才能找到数据库地址
//@SpringBootTest
//@TestPropertySource(locations = "classpath:bootstrap.yaml")
@Slf4j
public class EsSearchTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService iItemService;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("192.168.112.128:9200")
        ));
    }

    @Test
    void matchAllTest() throws IOException {
        //创建request对象
        SearchRequest request = new SearchRequest("items");
        //配置request参数
        request.source()
                .query(QueryBuilders.matchAllQuery());
        //发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponse(response);
    }


    //利用javaRestClient实现搜索关键字为脱脂奶 品牌为德亚 价格不超过300
    @Test
    void complexSearchTest() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source()
                .query(QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("name", "脱脂奶"))
                        .filter(QueryBuilders.termQuery("brand", "德亚"))
                        .filter(QueryBuilders.rangeQuery("price").lte(30000)));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponse(response);
    }

    //分页查询
    @Test
    void pageQueryTest() throws IOException {
        int pageNo = 1;
        int size = 10;
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchQuery("name", "脱脂奶")).from((pageNo - 1) * size).size(size);
        request.source().sort("sold", SortOrder.DESC);
        request.source().sort("price", SortOrder.DESC);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        parseResponse(response);
    }

    //高亮显示
    @Test
    void highLightTest() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(new BoolQueryBuilder()
                        .must(QueryBuilders.matchQuery("name", "脱脂奶"))
                        .filter(QueryBuilders.termQuery("brand", "德亚")))
                .sort("sold", SortOrder.DESC);
        request.source().highlighter(new HighlightBuilder().field("name").preTags("<em>").postTags("</em>"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Arrays.stream(response.getHits().getHits()).forEach(hit -> {
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField name = highlightFields.get("name");
            System.err.println(name.getFragments()[0].toString());
        });
    }

    //聚合查询
    @Test
    void aggTest() throws IOException {
        SearchRequest request = new SearchRequest("items");
        request.source().query(QueryBuilders.matchAllQuery());
        request.source().size(0);
        String aggName = "brandAgg";
        request.source().aggregation(AggregationBuilders.terms(aggName).field("brand"));
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        Aggregations aggregations = response.getAggregations();
        Terms aggregation = (Terms) aggregations.get(aggName);
        List<? extends Terms.Bucket> buckets = aggregation.getBuckets();
        for(var bucket:buckets){
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        }
    }

    private static void parseResponse(SearchResponse response) {
        SearchHits hitss = response.getHits();
        long value = hitss.getTotalHits().value;
        System.err.println(value);
        SearchHit[] hits = hitss.getHits();
        for (SearchHit hit : hits) {
            System.err.println(hit.getSourceAsString());
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (this.client != null) {
            this.client.close();
        }
    }
}
