#!/bin/bash

echo "============================================="
echo "HDFS 高可用集群部署脚本"
echo "============================================="

CLUSTER_NAME="mycluster"
NN1="node1"
NN2="node2"
JOURNAL_NODES=("node1" "node2" "node3")
ZK_NODES=("node1" "node2" "node3")

echo "[1/6] 检查并创建目录..."
for node in "${JOURNAL_NODES[@]}"; do
  ssh $node "mkdir -p /opt/hadoop/tmp /opt/hadoop/namenode /opt/hadoop/datanode /opt/hadoop/journal /opt/hadoop/logs /opt/hadoop/pids"
done

echo ""
echo "[2/6] 启动 JournalNode..."
for node in "${JOURNAL_NODES[@]}"; do
  ssh $node "hdfs --daemon start journalnode"
done
sleep 5

echo ""
echo "[3/6] 在 ${NN1} 上格式化 NameNode..."
ssh $NN1 "hdfs namenode -format -force"

echo ""
echo "[4/6] 在 ${NN1} 上格式化 ZKFC..."
ssh $NN1 "hdfs zkfc -formatZK"

echo ""
echo "[5/6] 启动 ${NN1} 上的 NameNode..."
ssh $NN1 "hdfs --daemon start namenode"

echo ""
echo "[6/6] 在 ${NN2} 上同步元数据并启动 NameNode..."
ssh $NN2 "hdfs namenode -bootstrapStandby"
ssh $NN2 "hdfs --daemon start namenode"

echo ""
echo "启动 ZKFC 服务..."
ssh $NN1 "hdfs --daemon start zkfc"
ssh $NN2 "hdfs --daemon start zkfc"

echo ""
echo "启动 DataNode 服务..."
hdfs --workers --daemon start datanode

echo ""
echo "============================================="
echo "HDFS 高可用集群部署完成！"
echo "访问 Web UI："
echo "  NameNode 1: http://${NN1}:50070"
echo "  NameNode 2: http://${NN2}:50070"
echo "============================================="
