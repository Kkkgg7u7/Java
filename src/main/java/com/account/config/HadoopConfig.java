package com.account.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.net.URI;

@Slf4j
@org.springframework.context.annotation.Configuration
public class HadoopConfig {

    @Value("${hadoop.hdfs.uri}")
    private String hdfsUri;

    @Value("${hadoop.hdfs.user:hdfs}")
    private String hdfsUser;

    @Bean
    public FileSystem fileSystem() {
        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", hdfsUri);
            conf.set("dfs.replication", "3");
            conf.set("dfs.client.use.datanode.hostname", "true");

            System.setProperty("HADOOP_USER_NAME", hdfsUser);

            URI uri = new URI(hdfsUri);
            FileSystem fs = FileSystem.get(uri, conf, hdfsUser);
            log.info("HDFS FileSystem initialized successfully, uri: {}", hdfsUri);
            return fs;
        } catch (Exception e) {
            log.error("Failed to initialize HDFS FileSystem, uri: {}", hdfsUri, e);
            return null;
        }
    }
}
