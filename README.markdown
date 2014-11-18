# Storm Sample Project #

This is a set of projects for demonstrating and testing an architecture mixing Kafka, Hadoop, Storm, Redis and node.js by performing Twitter sentiment analysis.

# How to use this project?

This project contains a simple Storm topology as described on [ZData](http://www.zdatainc.com/2014/07/real-time-streaming-apache-storm-apache-kafka/) blog.

In order to use it, you will need

* Access to the Twitter Streaming API: using your credentials, connect to twitter and create an application to obtain credentials.
* A Kafka Cluster (>0.8): Kafka will be used to connect to Twitter, consume the stream and make it available for the Storm Kafka Spout.
* A Storm Cluster (>0.9): The cluster shall have more than 4 worker nodes, and one Nimbus management node.
* A node.js server, which can be hosted separately or on the same machine. This will be used to collect the analyzed tweets and display their analysis.

The source website mentions it uses a CentOS machine with virtual machines and containers but doesn't provide a simple way to consume the "tutorial". This simple project aims at providing a comprehensive deployment to enjoy Twitter Sentiment Analysis running with Ubuntu.

# Maintainer

Well obviously zdatainc does something with this project but on the Canonical side, the maintainer will be Samuel Cozannet <samnco@gmail.com>

# Preferred deployment method

This project should be consumed via [Juju](http://juju.ubuntu.com) through the deployment of a bundle.

The below instruction will however explain how to use it in a non automated environment

# Prerequisites

In order to run this project you will have to compile it. You will thus need maven and OpenJDK. Install the following packages:

    :~# apt-get install maven

## Storm Configuration
### Prerequisites

Assuming you deployed a Storm cluster containing at least 4 storm workers and a Nimbus controller, connect on any node and apply the following:

    :~# git clone https://github.com/SaMnCo/StormSampleProject.git storm-sentiment-analysis

You'll note that this code is actually different from the original (pre-fork). The pom.xml file has been modified and is different from the original document.

### Configuration

Once you do that, you'll have to edit the configuration file with your favorite editor. This file is located in **./rts.storm/src/main/resources** (from the project root) and named **rts.storm.properties**. It contains the below elements:

    :~# cd storm-sentiment-analysis
    :~# cat /rts.storm/src/main/resources/rts.storm.properties
    rts.storm.workers: 4
    rts.storm.zkhosts: ZOOKEEPER_CLUSTER_ADDRESS:ZOOKEEPER_CLUSTER_PORT
    rts.storm.webserv: NODEJS_APP_URL:NODEJS_APP_PORT:NODEJS_APP_URI
    rts.storm.kafka_topic: twitter.live
    rts.storm.hdfs_output_dir: hdfs:///user/storm/${rts.storm.kafka_topic}
    rts.storm.hdfs_output_file: ${rts.storm.hdfs_output_dir}/scores

* rts.storm.workers: is the number of nodes in your storm cluster that will be consumed to perform the topology. If you deploy less than this number of nodes, the topology will fail to run and let you know you exhausted the resources.
* rts.storm.zkhosts: this is the entry point of the kafka_spout. The Storm topology will ask your ZooKeeper cluster which source it can consumer. On the other side (kafka server/broker) you can only mention 1 server even if you run a HA ZooKeeper cluster. So make sure to input the same or you may end up with a problem where your Kafka Producers are not declared to the topology. Obviously this doesn't scale to production (yet) but it will be OK for demonstrations and PoCs.
* rts.storm.webserv: <Full HTTP URL of the node.js app>.
* rts.storm.kafka_topic: Name of the twitter feed you set. Do not change this if you use my configuration as it is hard coded in the Kafka configuration for now.
* rts.storm.hdfs_output_dir: Folder where the output of the topology will be stored. Note this requires the Storm Nodes to be located on the same filesystem as the HDFS nodes.
* rts.storm.hdfs_output_file: Name of the output file. Node how you can use variables from this very same configuration file. This requires the Storm Workers to be colocated on HDFS nodes. Current status of the charms for Hadoop prevent this to happen for now.

### Other configuration points

There are 3 files that are used by the topology to score the tweets, all stored in the same *./rts.storm/src/main/resources** folder:

* neg-words: is the list of words that will be used when giving a negative score. The more words from that list in the tweet the higher the negative score will be.
* pos-words: same for the positive sentiments
* stop-words: those words are considered neutral and are removed from the tweets before analysis.

You can edit those lists prior to compilation as needed for an opinionated analysis.  

### Compilation

Once you are happy with the configuration just run

    :~# cd storm-sentiment-analysis
    :~# maven package

The result of this will be the creation of all necessary paths and files to generated the .jar file that is the topology.

    :~# storm-sentiment-analysis/rts.storm$ tree ./ -L 2
        ./
        ├── com
        │   ├── fasterxml
        │   ├── google
        │   └── zdatainc
        ├── dependency-reduced-pom.xml
        ├── kafka
        │   ├── admin
        │   ├── cluster
        │   ├── common
        │   ├── controller
        │   ├── Kafka$$anon$1.class
        │   ├── Kafka$$anonfun$main$1.class
        │   ├── Kafka.class
        │   ├── Kafka$.class
        │   ├── log
        │   ├── message
        │   ├── tools
        │   └── utils
        ├── META-INF
        │   ├── DEPENDENCIES
        │   ├── LICENSE
        │   ├── LICENSE.txt
        │   ├── MANIFEST.MF
        │   ├── maven
        │   ├── NOTICE
        │   ├── NOTICE.txt
        │   └── services
        ├── neg-words.txt
        ├── org
        │   └── apache
        ├── pom.xml
        ├── pos-words.txt
        ├── README.markdown
        ├── rts.storm.properties
        ├── src
        │   └── main
        ├── stop-words.txt
        └── target
            ├── classes
            ├── maven-archiver
            ├── original-rts.storm-0.0.1.jar
            └── rts.storm-0.0.1.jar

# Running the project
## Starting all elements

1. Start the Kafka Server (Broker)

  1.1. Manually

See the related project for more information. To start it manually you can do:

    :~# cd /opt/kafka
    :~# ./bin/kafka-server-start.sh /opt/kafka/config/server.properties

<That makes sure you have a broker ready to welcome the stream of data from Twitter.

The lines of log should end like:

    root@kafka-0:/opt/kafka# ./bin/kafka-server-start.sh /opt/kafka/config/server.properties
    [2014-10-20 07:55:59,468] INFO zookeeper state changed (SyncConnected) (org.I0Itec.zkclient.ZkClient)
    [2014-10-20 07:56:00,287] INFO Found clean shutdown file. Skipping recovery for all logs in data directory '/tmp/kafka-logs' (kafka.log.LogManager)
    [2014-10-20 07:56:00,289] INFO Loading log 'twitter.live-1' (kafka.log.LogManager)
    [2014-10-20 07:56:00,394] INFO Completed load of log twitter.live-1 with log end offset 369524 (kafka.log.Log)
    SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
    SLF4J: Defaulting to no-operation (NOP) logger implementation
    SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
    [2014-10-20 07:56:00,475] INFO Loading log 'twitter.live-0' (kafka.log.LogManager)
    [2014-10-20 07:56:00,481] INFO Completed load of log twitter.live-0 with log end offset 647391 (kafka.log.Log)
    [2014-10-20 07:56:00,483] INFO Starting log cleanup with a period of 60000 ms. (kafka.log.LogManager)
    [2014-10-20 07:56:00,489] INFO Starting log flusher with a default period of 9223372036854775807 ms. (kafka.log.LogManager)
    [2014-10-20 07:56:00,543] INFO Awaiting socket connections on 0.0.0.0:9092. (kafka.network.Acceptor)
    [2014-10-20 07:56:00,544] INFO [Socket Server on Broker 0], Started (kafka.network.SocketServer)
    [2014-10-20 07:56:00,827] INFO Will not load MX4J, mx4j-tools.jar is not in the classpath (kafka.utils.Mx4jLoader$)
    [2014-10-20 07:56:00,887] INFO 0 successfully elected as leader (kafka.server.ZookeeperLeaderElector)
    [2014-10-20 07:56:01,614] INFO Registered broker 0 at path /brokers/ids/0 with address ip-1XXXXXXXXXXXXXX-compute.internal:9092. (kafka.utils.ZkUtils$)
    [2014-10-20 07:56:01,691] INFO New leader is 0 (kafka.server.ZookeeperLeaderElector$LeaderChangeListener)
    [2014-10-20 07:56:01,758] INFO [Kafka Server 0], started (kafka.server.KafkaServer)
    [2014-10-20 07:56:02,792] INFO [ReplicaFetcherManager on broker 0] Removed fetcher for partitions [twitter.live,0],[twitter.live,1] (kafka.server.ReplicaFetcherManager)
    [2014-10-20 07:56:03,255] INFO [ReplicaFetcherManager on broker 0] Removed fetcher for partitions [twitter.live,0],[twitter.live,1] (kafka.server.ReplicaFetcherManager)

  1.2. As a service

Assuming this was deployed with Juju, Kafka would be a daemon started automatically:

    root@kafka-0:~# service kafka <start | stop | restart>

Note this require your Zookeeper cluster to be up & running.

2. Start the Kafka Producer

  2.1. Manually

In  the Kafka language, Producer means your "data source collection hub". It's the primary Kafka node that connects to your raw data source, in our case a Twitter Streaming API feed.

For this we use an old version of https://github.com/NFLabs/kafka-twitter.git which we refactored a little bit (see https://github.com/SaMnCo/charm-kafka-twitter)

See the related project for more information but assuming this was deployed with Juju on the same node as you Kafka Server, you can start it in command line with

    :~# cd /opt/kafka-twitter
    :~# ./gradlew run -Pargs="/opt/kafka-twitter/conf/producer.conf"

At some point you will then see a [75% - run] notification, and lines mentionning you are connected to Twitter.

The output should then be:

    root@kafka-0:/opt/kafka-twitter# ./gradlew run -Pargs="/opt/kafka-twitter/conf/producer.conf"
    :compileJava UP-TO-DATE
    :processResources UP-TO-DATE
    :classes UP-TO-DATE
    :run
    log4j:WARN No appenders could be found for logger (kafka.utils.VerifiableProperties).
    log4j:WARN Please initialize the log4j system properly.
    5854 [Twitter Stream consumer-1[initializing]] INFO twitter4j.TwitterStreamImpl - Establishing connection.
    12963 [Twitter Stream consumer-1[Establishing connection]] INFO twitter4j.TwitterStreamImpl - Connection established.
    12963 [Twitter Stream consumer-1[Establishing connection]] INFO twitter4j.TwitterStreamImpl - Receiving status stream.
    > Building 75% > :run

  2.2. As a service

In the latest version of the project this has been converted to a service which you can start with

    root@kafka-0:~# service kafka-twitter <start | stop | restart>

This require a Kafka Broker to be running, but it doesn't have to be on the same node.

  2.3 Testing

If you want to check if it really works, Kafka hosts a log of what it does in /tmp

    ubuntu@kafka-0:~$ ls -la /tmp/kafka-logs/twitter.live-0/
    total 2260880
    drwxr-xr-x 2 root root      4096 Oct 16 14:50 .
    drwxr-xr-x 4 root root      4096 Oct 20 08:01 ..
    -rw-r--r-- 1 root root    734256 Oct 20 08:01 00000000000000000000.index
    -rw-r--r-- 1 root root 536871014 Oct 15 15:00 00000000000000000000.log
    -rw-r--r-- 1 root root    728016 Oct 20 08:01 00000000000000151393.index
    -rw-r--r-- 1 root root 536871292 Oct 16 12:16 00000000000000151393.log
    -rw-r--r-- 1 root root    732488 Oct 20 08:01 00000000000000302151.index
    -rw-r--r-- 1 root root 536877126 Oct 16 14:00 00000000000000302151.log
    -rw-r--r-- 1 root root    732600 Oct 20 08:01 00000000000000454244.index
    -rw-r--r-- 1 root root 536874264 Oct 16 14:50 00000000000000454244.log
    -rw-r--r-- 1 root root    224728 Oct 20 08:01 00000000000000605240.index
    -rw-r--r-- 1 root root 164450494 Oct 20 08:01 00000000000000605240.log

As you can see, each log batch is 512MB then it gets rotated. However the old logs are kept so beware of the disk beast. You can change that in the Kafka Configuration. (see the [kafka-twitter](https://github.com/SaMnCo/charm-kafka-twitter) project)

3. Start the Storm Topology and grep for the output

3.1. Manually

Now get back to your storm node, and load the topology from the CLI:

    root@storm-nimbus:storm-sentiment-analysis/rts.storm# /usr/lib/storm/bin/storm jar target/rts.storm-0.0.1.jar com.zdatainc.rts.storm.SentimentAnalysisTopology | grep "Sent"
    Sent:{"id": "524109687575171072", "text": "rt andreacortejo share  message    a chance  win wonderful prizes surpriseyourself httptcobqtwcvo", "pos": "0.000000", "neg": "0.125000", "score": "negative" }
    Sent:{"id": "524109687571349504", "text": "rt louistomlinson    bought  copy   album   loves face   httptcocyrpuz welivetogetherdea", "pos": "0.052632", "neg": "0.000000", "score": "positive" }
    Sent:{"id": "524109687571349505", "text": "rt denomfalme  mashujaa day  celebrating evedsouza   influence      hosting hitsnothomework salutehiphopra", "pos": "0.000000", "neg": "0.000000", "score": "negative" }
    Sent:{"id": "524109687587758082", "text": "rt laterreading tales   dragonfly book  tandem  author tamara ferguson tammysdragonfly", "pos": "0.000000", "neg": "0.000000", "score": "negative" }

As you can see the results are a bit weird for now, but working on it. If you remove the grep part of the command you'll have a very verbose output.

3.2 As a service

You should then load the topology within Storm with:

    root@storm-nimbus:storm-sentiment-analysis/rts.storm# /usr/lib/storm/bin/storm jar target/rts.storm-0.0.1.jar com.zdatainc.rts.storm.SentimentAnalysisTopology sentiment-analysis

You should then access your Storm UI on port 8080 of the nimbus server to see the topology loaded and running.

# Editing the code

All the code for this topology is hosted in **rts.storm/src/main/java/com/zdatainc/rts/storm** from the root of the project. It is made of a handful of java files (Storm Bolts and Spouts), a configuration "Properties.java" file and a topology definition **SentimentAnalysisTopology.java**

    root@storm-nimbus:storm-sentiment-analysis/rts.storm/src/main/java/com/zdatainc/rts/storm# ls -la
    total 72
    drwxr-xr-x 2 root root 4096 Oct 15 11:05 .
    drwxr-xr-x 3 root root 4096 Oct 15 11:05 ..
    -rw-r--r-- 1 root root 3379 Oct 15 11:05 HDFSBolt.java
    -rw-r--r-- 1 root root 2990 Oct 15 11:05 JoinSentimentsBolt.java
    -rw-r--r-- 1 root root 1356 Oct 15 11:05 NegativeSentimentBolt.java
    -rw-r--r-- 1 root root 1386 Oct 15 11:05 NegativeWords.java
    -rw-r--r-- 1 root root 3777 Oct 16 14:38 NodeNotifierBolt.java
    -rw-r--r-- 1 root root  673 Oct 15 11:05 Pair.java
    -rw-r--r-- 1 root root 1357 Oct 15 11:05 PositiveSentimentBolt.java
    -rw-r--r-- 1 root root 1386 Oct 15 11:05 PositiveWords.java
    -rw-r--r-- 1 root root 1061 Oct 15 11:05 Properties.java
    -rw-r--r-- 1 root root 3281 Oct 15 13:34 SentimentAnalysisTopology.java
    -rw-r--r-- 1 root root 1355 Oct 15 11:05 SentimentScoringBolt.java
    -rw-r--r-- 1 root root 1257 Oct 15 11:05 StemmingBolt.java
    -rw-r--r-- 1 root root 1385 Oct 15 11:05 StopWords.java
    -rw-r--r-- 1 root root 1141 Oct 15 11:05 TextFilterBolt.java
    -rw-r--r-- 1 root root  881 Oct 15 11:05 Triple.java
    -rw-r--r-- 1 root root 2020 Oct 15 11:05 TwitterFilterBolt.java

Let's have a look at the section for the topology in **SentimentAnalysisTopology.java** file to see how it looks like:

    private static StormTopology createTopology()
    {
        SpoutConfig kafkaConf = new SpoutConfig(
            new ZkHosts(Properties.getString("rts.storm.zkhosts")),
            KAFKA_TOPIC,
            "/kafka",
            "KafkaSpout");
        kafkaConf.scheme = new SchemeAsMultiScheme(new StringScheme());
        TopologyBuilder topology = new TopologyBuilder();

        topology.setSpout("kafka_spout", new KafkaSpout(kafkaConf), 4);

        topology.setBolt("twitter_filter", new TwitterFilterBolt(), 4)
                .shuffleGrouping("kafka_spout");

        topology.setBolt("text_filter", new TextFilterBolt(), 4)
                .shuffleGrouping("twitter_filter");

        topology.setBolt("stemming", new StemmingBolt(), 4)
                .shuffleGrouping("text_filter");

        topology.setBolt("positive", new PositiveSentimentBolt(), 4)
                .shuffleGrouping("stemming");
        topology.setBolt("negative", new NegativeSentimentBolt(), 4)
                .shuffleGrouping("stemming");

        topology.setBolt("join", new JoinSentimentsBolt(), 4)
                .fieldsGrouping("positive", new Fields("tweet_id"))
                .fieldsGrouping("negative", new Fields("tweet_id"));

        topology.setBolt("score", new SentimentScoringBolt(), 4)
                .shuffleGrouping("join");

        /** topology.setBolt("hdfs", new HDFSBolt(), 4)
                .shuffleGrouping("score"); **/
        topology.setBolt("nodejs", new NodeNotifierBolt(), 4)
                .shuffleGrouping("score");

        return topology.createTopology();
    }

So first we can see the configuration is loaded and the ZooKeeper cluster is identified. This is to set the kafka_spout configuration, which is the only Spout that is not  directly embedded in this folder (comes as a native extension of Storm).

Then we can understand the graph that was presented on the main page of ZDataInc

We have:

1. Run kafka_spout: this will listen to the twitter feed. In the original design this was supposed to come from a flat file, but we made it happen from a live feed. Which is better as it allows to identify some errors in the original topology.

2. Run twitter_filter: this filter will eliminate any tweet that is not in English to make sure our stop-words, pos-words and neg-words are OK.

3. Run text_filter: This removes all ugly characters from the text and sets everything to lowercase.

4. Run stemming: this will remove all stop-words from tweet text

5. Run positive AND negative: the analysis of positive and negative is done in parallel and each tweet is evaluated by both bolts.

6. Run join: this will take the outputs from positive and negative and unify (join) them.

7. Run score: This will compute the final positive / negative score by comparing the respective scores of both.

8. Run hdfs and nodejs: Note HDFS is commented out for now. It should copy results to filesystem for reusing later. The nodejs is called but buggy for now. It is supposed to notify the application server so the latter display the tweet.

# Troubleshooting / FAQ
## Including Java version

At first run, Maven would not compile because of a Java versioning problem. This was fixed by adding

      <plugin>  
        <groupId>org.apache.maven.plugins</groupId>  
        <artifactId>maven-compiler-plugin</artifactId>  
        <configuration>  
          <source>1.7</source>  
          <target>1.7</target>  
        </configuration>  
      </plugin>

to the original pom.xml.

## Include java-util

While doing some testing on the Java file, we had to use java-util. This code is not used anymore but we kept the dependency just in case in the pom.xml file:

    <dependency>
      <groupId>com.cedarsoftware</groupId>
      <artifactId>java-util</artifactId>
      <version>1.12.0</version>
      <scope>provided</scope>
    </dependency>

## Http Components Versioning

This may be the root cause of the NodeNotifier not working. The Java Httpcomponents library has evolved a lot since this project was written. We had to update the versioning of the pom.xml file:

    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpcore</artifactId>
      <version>4.3.2</version>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.apache.httpcomponents</groupId>
      <artifactId>httpclient</artifactId>
      <version>4.3.2</version>
      <scope>provided</scope>
    </dependency>

## HTTP Libraries not updated on Storm Workers

By default, Hortonworks Storm Workers load versions 4.1.1 of HTTP Components. Make sure you run the following commands from your computer (or locally, this is pretty straightforward)

    :~$ juju run --service=storm-worker "wget http://central.maven.org/maven2/org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar -O /usr/lib/storm/lib/httpcore-4.3.2.jar"
    :~$ juju run --service=storm-worker "wget http://central.maven.org/maven2/org/apache/httpcomponents/httpclient/4.3.2/httpclient-4.3.2.jar -O /usr/lib/storm/lib/httpclient-4.3.2.jar"
    :~$ juju run --service=storm-worker "rm -f /usr/lib/storm/lib/httpclient-4.1.1.jar /usr/lib/storm/lib/httpcore-4.1.jar"
    :~$ juju run --service=nimbus-server "wget http://central.maven.org/maven2/org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar -O /usr/lib/storm/lib/httpcore-4.3.2.jar"
    :~$ juju run --service=nimbus-server "wget http://central.maven.org/maven2/org/apache/httpcomponents/httpclient/4.3.2/httpclient-4.3.2.jar -O /usr/lib/storm/lib/httpclient-4.3.2.jar"
    :~$ juju run --service=nimbus-server "rm -f /usr/lib/storm/lib/httpclient-4.1.1.jar /usr/lib/storm/lib/httpcore-4.1"
    :~$ juju run --service=storm-worker "supervisorctl stop all"
    :~$ juju run --service=nimbus-server "supervisorctl restart all"
    :~$ juju run --service=storm-worker "supervisorctl start all"

Then you'll have the right versions running. This has been reported as a bug in the charms and will be updated soon.

## Topology failing after a few tweets

It happens that the default netty configuration loaded with Hortonworks Storm and Juju is very picky. The below lines shall be added to /etc/storm/conf/storm.yaml

    storm.messaging.netty.max_retries: 300
    storm.messaging.netty.max_wait_ms: 2000
    storm.messaging.netty.min_wait_ms: 100

## Failure after a few days

ZooKeeper nodes tend to fail after a few days if nothing is done. This is because ZK keeps logging information for ever and doesn't cleanup logs by default. This behavior can be changed by adding this to the crontab:

    0 0 * * * /usr/lib/zookeeper/bin/zkCleanup.sh -n 3

Then restart the Cron service.

   root@hdp-zookeper:~$# service cron restart

## Failure to store on HDFS

The current status of the Charms to deploy Hortonworks Hadoop distribution prevent colocation of services, which is required for this example. This has been reported as a bug and shall be fixed soon.

## License ##

The code in this project is made available as free and open source software
under the terms and conditions of the GNU Public License. For more information,
please refer to the LICENSE text file included with this project, or visit
[gnu.org][1] if the license file was not included.

[1]: http://www.gnu.org/licenses/gpl.html
