package com.hmall.item;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domain.po.Item;
import com.hmall.item.domain.po.ItemDoc;
import com.hmall.item.service.IItemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

//指明才能找到数据库地址
@SpringBootTest
@TestPropertySource(locations = "classpath:bootstrap.yaml")
@Slf4j
public class EsDocumentsTest {
    private RestHighLevelClient client;

    @Autowired
    private IItemService iItemService;

    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("192.168.112.128:9200")
        ));
    }

    //新增数据 存在覆盖 不存在新增
    @Test
    void testIndexDoc() throws IOException {
        Item item = iItemService.getById(546872L);
        ItemDoc itemDoc = BeanUtil.copyProperties(item, ItemDoc.class);
        IndexRequest request = new IndexRequest("items").id(String.valueOf(item.getId()));
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        //直接新增 不需要通过indices
        client.index(request, RequestOptions.DEFAULT);
    }

    //查看数据
    @Test
    void testGetDoc() throws IOException {
        GetRequest items = new GetRequest("items").id("546872");
        GetResponse response = client.get(items, RequestOptions.DEFAULT);
        String source = response.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(source, ItemDoc.class);
        System.err.println(itemDoc);
    }

    //删除文档
    @Test
    void testDelete() throws IOException {
        DeleteRequest request = new DeleteRequest("items", "546872");
        client.delete(request, RequestOptions.DEFAULT);
    }

    //修改文档
    @Test
    void testUpdate() throws IOException {
        UpdateRequest request = new UpdateRequest("items", "546872");
        ItemDoc itemDoc = new ItemDoc();
        itemDoc.setName("zhangsan");
        request.doc(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        client.update(request, RequestOptions.DEFAULT);
    }

    //批量处理文档
    @Test
    void testBulk() throws IOException {

        int pageNo = 1;
        int size = 500;
        while (true) {
            Page<Item> page = iItemService.lambdaQuery()
                    .eq(Item::getStatus, 1)
                    .page(new Page<>(pageNo, size));
            List<Item> records = page.getRecords();
            if(CollUtil.isEmpty(records)){
                return;
            }
            BulkRequest request = new BulkRequest();
            for(var item:records){
                request.add(new IndexRequest("items").id(item.getId().toString()).source(
                        JSONUtil.toJsonStr(BeanUtil.copyProperties(item,ItemDoc.class)),XContentType.JSON
                ));
            }
            client.bulk(request, RequestOptions.DEFAULT);
            pageNo++;
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (this.client != null) {
            this.client.close();
        }
    }
}
