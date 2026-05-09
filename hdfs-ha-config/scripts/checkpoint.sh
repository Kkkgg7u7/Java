#!/bin/bash

echo "======================================"
echo "手动触发 NameNode Checkpoint"
echo "======================================"

ACTIVE_NN=""
for nn in nn1 nn2; do
    state=$(hdfs haadmin -getServiceState "${nn}" 2>/dev/null)
    if [ "${state}" = "active" ]; then
        ACTIVE_NN="${nn}"
        break
    fi
done

if [ -z "${ACTIVE_NN}" ]; then
    echo "错误: 未找到 Active NameNode"
    exit 1
fi

echo "Active NameNode: ${ACTIVE_NN}"
echo ""

echo "正在触发 Checkpoint..."
hdfs dfsadmin -saveNamespace

if [ $? -eq 0 ]; then
    echo "✓ Checkpoint 触发成功"
    echo ""
    echo "检查点信息:"
    hdfs dfsadmin -rollEdits
    echo "✓ 编辑日志已滚动"
else
    echo "✗ Checkpoint 触发失败"
    exit 1
fi
