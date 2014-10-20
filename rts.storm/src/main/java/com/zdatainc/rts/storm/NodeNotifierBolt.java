package com.zdatainc.rts.storm;

/**
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;  
import org.apache.http.impl.client.HttpClients;  
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
**/
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
    /**
    private CloseableHttpClient httpclient;
    **/
    private void reconnect()
    {
        this.client = HttpClientBuilder.create().build();
        /**
        this.httpclient = HttpClients.createDefault();
        **/
    }

    public void execute(Tuple input, BasicOutputCollector collector)
    {
        Long id = input.getLong(input.fieldIndex("tweet_id"));
        /**
        String idStr = Long.toString(id);
        **/
        String tweet = input.getString(input.fieldIndex("tweet_text"));
        Float pos = input.getFloat(input.fieldIndex("pos_score"));
        Float neg = input.getFloat(input.fieldIndex("neg_score"));
        /**
        String posStr = Float.toString(pos);
        String negStr = Float.toString(neg);
        **/
        String score = input.getString(input.fieldIndex("score"));
        HttpPost post = new HttpPost(this.webserver);
        String content = String.format(
            "{\"id\": \"%d\", "  +
            "\"text\": \"%s\", " +
            "\"pos\": \"%f\", "  +
            "\"neg\": \"%f\", "  +
            "\"score\": \"%s\" }",
            id, tweet, pos, neg, score);
        /**
        List<NameValuePair> contentList = new ArrayList<NameValuePair>();
        contentList.add(new BasicNameValuePair("id", idStr));
        contentList.add(new BasicNameValuePair("text", tweet));
        contentList.add(new BasicNameValuePair("pos", posStr));
        contentList.add(new BasicNameValuePair("pos", negStr));
        contentList.add(new BasicNameValuePair("score", score));
        **/

        try
        {
            System.out.println("Sent:" + content);
            post.setEntity(new StringEntity(content));
            /**
            post.setEntity(new UrlEncodedFormEntity(contentList, Consts.UTF_8));
            **/
            HttpResponse response = client.execute(post); 
            System.out.println("Got:" + response);
            /**
            CloseableHttpResponse response = httpclient.execute(post);
            **/
            org.apache.http.util.EntityUtils.consume(response.getEntity());
            /**
            HttpEntity entity = response.getEntity();
            response.close();
            **/
        }
        catch (Exception ex)
        {
            LOGGER.error("exception thrown while attempting post", ex);
            LOGGER.trace(null, ex);
            reconnect();
        }
        /** finally {
            // Closing the response
        } **/
    }

    public void declareOutputFields(OutputFieldsDeclarer declarer) { }
}
