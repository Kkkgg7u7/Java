#!/bin/bash

NODES=("node1" "node2" "node3")
SLEEP_INTERVAL=60

echo "======================================"
echo "滚动重启 DataNode"
echo "======================================"
echo "节点列表: ${NODES[*]}"
echo "重启间隔: ${SLEEP_INTERVAL}秒"
echo ""

read -p "确认继续? (yes/no): " confirm

if [ "${confirm}" != "yes" ]; then
    echo "取消操作"
    exit 0
fi

for node in "${NODES[@]}"; do
    echo ""
    echo "--------------------------------------"
    echo "重启节点: ${node}"
    echo "--------------------------------------"

    echo "停止 DataNode..."
    ssh "${node}" "hdfs --daemon stop datanode"
    sleep 5

    echo "启动 DataNode..."
    ssh "${node}" "hdfs --daemon start datanode"
    sleep 10

    if ssh "${node}" "jps | grep -q DataNode"; then
        echo "✓ ${node}: DataNode 已重启"
    else
        echo "✗ ${node}: DataNode 启动失败"
    fi

    if [ "${node}" != "${NODES[-1]}" ]; then
        echo ""
        echo "等待 ${SLEEP_INTERVAL} 秒..."
        sleep "${SLEEP_INTERVAL}"
    fi
done

echo ""
echo "======================================"
echo "滚动重启完成!"
echo "======================================"
