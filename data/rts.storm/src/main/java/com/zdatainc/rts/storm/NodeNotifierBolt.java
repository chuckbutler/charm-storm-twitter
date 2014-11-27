package com.zdatainc.rts.storm;

import org.apache.log4j.Logger;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Tuple;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class NodeNotifierBolt extends BaseBasicBolt
{
    private static final long serialVersionUID = 42L;
    private static final Logger LOGGER =
        Logger.getLogger(NodeNotifierBolt.class);
    private String webserver = Properties.getString("rts.storm.webserv");
    private HttpClient client;
    private void reconnect()
    {
        this.client = HttpClientBuilder.create().build();
    }

    public void execute(Tuple input, BasicOutputCollector collector)
    {
        Long id = input.getLong(input.fieldIndex("tweet_id"));
        String tweet = input.getString(input.fieldIndex("tweet_text"));
        Float pos = input.getFloat(input.fieldIndex("pos_score")) * 100;
        Float neg = input.getFloat(input.fieldIndex("neg_score")) * 100;
        String score = input.getString(input.fieldIndex("score"));
        HttpPost post = new HttpPost(this.webserver);
        String content = String.format(
            "{\"auth_token\": \"YOUR_AUTH_TOKEN\", " +
            "\"title\": \"%d\", "  +
            "\"text\": \"%s\", " +
            "\"moreinfo\": \"%s\", " +
            "\"pos\": %f, "  +
            "\"neg\": %f, "  +
            "\"score\": \"%s\" }",
            id, tweet, tweet, pos, neg, score);

        try
        {
            System.out.println("Sent:" + content);
            post.setEntity(new StringEntity(content));
            HttpResponse response = client.execute(post); 
            System.out.println("Got:" + response);
            org.apache.http.util.EntityUtils.consume(response.getEntity());
        }
        catch (Exception ex)
        {
            LOGGER.error("exception thrown while attempting post", ex);
            LOGGER.trace(null, ex);
            reconnect();
        }
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) { }
}
