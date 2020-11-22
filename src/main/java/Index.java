import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;

public class Index {


    public static void createIndex() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(new FileReader("Sections.json"));
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));

        try {
            client.indices().delete(new DeleteIndexRequest("documents"), RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            System.out.println("Index does not exist");
        }

        IndexRequest request = new IndexRequest("documents");
        System.out.println("Indexing...");
        for (Object o : jsonArray) {
            JSONObject object = (JSONObject) o;
            request.source(object.toJSONString(), XContentType.JSON);

            client.index(request, RequestOptions.DEFAULT);
        }

        CountRequest countRequest = new CountRequest();
        countRequest.source((new SearchSourceBuilder()).query(QueryBuilders.matchAllQuery()));
        CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
        System.out.println("Počet zaindexovaných dokumentov: " + countResponse.getCount());
        client.close();

    }
    public static void getSection(String name) throws IOException, ParseException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));

        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery("Name.keyword", name));
        searchRequest.indices("documents");
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        JSONParser parser = new JSONParser();
        JSONObject jsonResponse = (JSONObject) parser.parse(searchResponse.getHits().getHits()[0].getSourceAsString());
        System.out.println(jsonResponse.get("Name"));
        System.out.println(jsonResponse.get("Text"));
        client.close();
    }

}
