#!/bin/bash

echo "============================================="
echo "HDFS 高可用主备切换测试"
echo "============================================="

NN1="node1"
NN2="node2"
CLUSTER_NAME="mycluster"

echo ""
echo "[1/4] 查看当前 NameNode 状态..."
echo "NameNode ${NN1} 状态:"
ssh $NN1 "hdfs haadmin -getServiceState nn1"
echo "NameNode ${NN2} 状态:"
ssh $NN2 "hdfs haadmin -getServiceState nn2"

echo ""
echo "[2/4] 执行手动故障转移..."
echo "将 active 从 ${NN1} 切换到 ${NN2}..."
hdfs haadmin -failover nn1 nn2

echo ""
echo "[3/4] 再次查看 NameNode 状态..."
sleep 3
echo "NameNode ${NN1} 状态:"
ssh $NN1 "hdfs haadmin -getServiceState nn1"
echo "NameNode ${NN2} 状态:"
ssh $NN2 "hdfs haadmin -getServiceState nn2"

echo ""
echo "[4/4] 测试 HDFS 读写功能..."
echo "创建测试文件..."
hdfs dfs -mkdir -p /test
echo "Hello HDFS HA" > test.txt
hdfs dfs -put test.txt /test/
echo "读取测试文件..."
hdfs dfs -cat /test/test.txt
hdfs dfs -rm -r /test
rm -f test.txt

echo ""
echo "============================================="
echo "主备切换测试完成！"
echo "============================================="
