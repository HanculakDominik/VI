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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Index {
    /**
     * This method creates index and indices from json file.
     *
     * @param file      Name of file from which are json objects extracted for indexing
     * @param indexName Name of elasticsearch index
     * @throws IOException    Exceptions from file reading
     * @throws ParseException Exceptions from json parser
     */
    public static void createIndex(String file, String indexName) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(new FileReader(file));
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));

        try {
            client.indices().delete(new DeleteIndexRequest(indexName), RequestOptions.DEFAULT);
        } catch (ElasticsearchException e) {
            System.out.println("Index does not exist");
        }

        IndexRequest request = new IndexRequest(indexName);
        System.out.println("Indexing...");
        for (Object o : jsonArray) {
            JSONObject object = (JSONObject) o;
            request.source(object.toJSONString(), XContentType.JSON);

            client.index(request, RequestOptions.DEFAULT);
        }

        CountRequest countRequest = new CountRequest(indexName);
        countRequest.source((new SearchSourceBuilder()).query(QueryBuilders.matchAllQuery()));
        CountResponse countResponse = client.count(countRequest, RequestOptions.DEFAULT);
        System.out.println("Počet zaindexovaných dokumentov: " + countResponse.getCount());
        client.close();

    }

    /**
     * This method uses Search API of elastic to search sections using indexes.
     *
     * @param pageName    Name of page
     * @param sectionName Name of section
     * @param indexName   Name of index
     * @return List of json objects from response from elasticsearch
     * @throws IOException    Exceptions from RestHighLevelClient search
     * @throws ParseException Exceptions from json parser
     */
    public static ArrayList<JSONObject> getSection(String pageName, String sectionName, String indexName) throws IOException, ParseException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));

        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.size(1000);
        searchRequest.indices(indexName);


        if (pageName == null) {
            sourceBuilder.query(QueryBuilders.termQuery("SectionName.keyword", sectionName));
        } else if (sectionName == null) {
            sourceBuilder.query(QueryBuilders.termQuery("PageName.keyword", pageName));
        } else {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder
                    .filter(QueryBuilders.termQuery("PageName.keyword", pageName))
                    .filter(QueryBuilders.termQuery("SectionName.keyword", sectionName));
            sourceBuilder.query(boolQueryBuilder);
        }

        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        JSONParser parser = new JSONParser();

        SearchHit[] hits = searchResponse.getHits().getHits();
        ArrayList<JSONObject> returnValue = new ArrayList<>();
        for (SearchHit hit : hits) {
            returnValue.add((JSONObject) parser.parse(hit.getSourceAsString()));
        }
        client.close();
        return returnValue;
    }

    /**
     * This method returns array of statistics from field to print.
     *
     * @param field Name of field to returns statistics from
     * @return List of strings with keys and number of occurrences
     * @throws IOException Exceptions from RestHighLevelClient search
     */
    public static ArrayList<String> getStatistics(String field) throws IOException {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9200, "http"),
                        new HttpHost("localhost", 9201, "http")));

        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        TermsAggregationBuilder aggregation = AggregationBuilders.terms("stats");
        if(!field.equals("Level")) {

            aggregation.field(field + ".keyword");
        }else{
            aggregation.field(field);
        }
        sourceBuilder.aggregation(aggregation);
        searchRequest.indices("documents");
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Terms t = searchResponse.getAggregations().get("stats");
        ArrayList<String> returnValue = new ArrayList<>();
        for (Terms.Bucket term : t.getBuckets()) {
            returnValue.add(term.getKeyAsString() + " = " + term.getDocCount());
        }
        client.close();
        return returnValue;

    }

}
