#!/bin/bash

echo "停止 HDFS 高可用集群..."

hdfs --workers --daemon stop datanode

ssh node1 "hdfs --daemon stop zkfc"
ssh node2 "hdfs --daemon stop zkfc"

ssh node1 "hdfs --daemon stop namenode"
ssh node2 "hdfs --daemon stop namenode"

ssh node1 "hdfs --daemon stop journalnode"
ssh node2 "hdfs --daemon stop journalnode"
ssh node3 "hdfs --daemon stop journalnode"

echo "集群已停止！"
