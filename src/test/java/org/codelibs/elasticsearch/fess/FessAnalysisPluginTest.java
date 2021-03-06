package org.codelibs.elasticsearch.fess;

import static org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner.newConfigs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.codelibs.elasticsearch.runner.ElasticsearchClusterRunner;
import org.codelibs.elasticsearch.runner.net.Curl;
import org.codelibs.elasticsearch.runner.net.CurlResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.MatchQueryBuilder.Type;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FessAnalysisPluginTest {

    private ElasticsearchClusterRunner runner;

    private int numOfNode = 2;

    private int numOfDocs = 1000;

    private String clusterName;

    @Before
    public void setUp() throws Exception {
        clusterName = "es-kuromojineologd-" + System.currentTimeMillis();
        runner = new ElasticsearchClusterRunner();
        runner.onBuild(new ElasticsearchClusterRunner.Builder() {
            @Override
            public void build(final int number, final Builder settingsBuilder) {
                settingsBuilder.put("http.cors.enabled", true);
                settingsBuilder.put("http.cors.allow-origin", "*");
                settingsBuilder.put("index.number_of_shards", 1);
                settingsBuilder.put("index.number_of_replicas", 0);
                settingsBuilder.putArray("discovery.zen.ping.unicast.hosts", "localhost:9301-9310");
                settingsBuilder.put("plugin.types", "org.codelibs.elasticsearch.fess.FessAnalysisPlugin");
                settingsBuilder.put("index.unassigned.node_left.delayed_timeout", "0");
            }
        }).build(newConfigs().clusterName(clusterName).numOfNode(numOfNode));

    }

    @After
    public void cleanUp() throws Exception {
        runner.close();
        runner.clean();
    }

    @Test
    public void test_japanese() throws Exception {

        runner.ensureYellow();
        Node node = runner.node();

        final String index = "dataset";
        final String type = "item";

        final String indexSettings = "{\"index\":{\"analysis\":{" + "\"tokenizer\":{"//
                + "\"ja_user_dict\":{\"type\":\"fess_japanese_tokenizer\",\"mode\":\"extended\",\"user_dictionary\":\"userdict_ja.txt\"}"
                + "},"//
                + "\"analyzer\":{"
                + "\"ja_analyzer\":{\"type\":\"custom\",\"tokenizer\":\"ja_user_dict\",\"filter\":[\"fess_japanese_stemmer\"]}" + "}"//
                + "}}}";
        runner.createIndex(index, Settings.builder().loadFromSource(indexSettings).build());

        // create a mapping
        final XContentBuilder mappingBuilder = XContentFactory.jsonBuilder()//
                .startObject()//
                .startObject(type)//
                .startObject("properties")//

                // id
                .startObject("id")//
                .field("type", "string")//
                .field("index", "not_analyzed")//
                .endObject()//

                // msg1
                .startObject("msg")//
                .field("type", "string")//
                .field("analyzer", "ja_analyzer")//
                .endObject()//

                .endObject()//
                .endObject()//
                .endObject();
        runner.createMapping(index, type, mappingBuilder);

        final IndexResponse indexResponse1 = runner.insert(index, type, "1", "{\"msg\":\"東京スカイツリー\", \"id\":\"1\"}");
        assertTrue(indexResponse1.isCreated());
        runner.refresh();

        assertDocCount(0, index, type, "msg", "東京スカイツリー");

        try (CurlResponse response =
                Curl.post(node, "/" + index + "/_analyze").param("analyzer", "ja_analyzer").body("東京スカイツリー").execute()) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tokens = (List<Map<String, Object>>) response.getContentAsMap().get("tokens");
            assertEquals(0, tokens.size());
        }

    }

    private void assertDocCount(int expected, final String index, final String type, final String field, final String value) {
        final SearchResponse searchResponse =
                runner.search(index, type, QueryBuilders.matchQuery(field, value).type(Type.PHRASE), null, 0, numOfDocs);
        assertEquals(expected, searchResponse.getHits().getTotalHits());
    }
}
