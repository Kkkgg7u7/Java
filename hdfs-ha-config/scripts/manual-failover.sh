#!/bin/bash

CLUSTER_NAME="mycluster"

get_active_nn() {
    for nn in nn1 nn2; do
        state=$(hdfs haadmin -getServiceState "${nn}" 2>/dev/null)
        if [ "${state}" = "active" ]; then
            echo "${nn}"
            return 0
        fi
    done
    echo "none"
}

get_standby_nn() {
    for nn in nn1 nn2; do
        state=$(hdfs haadmin -getServiceState "${nn}" 2>/dev/null)
        if [ "${state}" = "standby" ]; then
            echo "${nn}"
            return 0
        fi
    done
    echo "none"
}

echo "======================================"
echo "HDFS 主备切换工具"
echo "======================================"

ACTIVE=$(get_active_nn)
STANDBY=$(get_standby_nn)

echo "当前状态:"
echo "  Active: ${ACTIVE}"
echo "  Standby: ${STANDBY}"
echo ""

if [ "${ACTIVE}" = "none" ] || [ "${STANDBY}" = "none" ]; then
    echo "错误: 无法获取正确的 NameNode 状态"
    exit 1
fi

read -p "确认将 Active 从 ${ACTIVE} 切换到 ${STANDBY}? (yes/no): " confirm

if [ "${confirm}" != "yes" ]; then
    echo "取消切换"
    exit 0
fi

echo ""
echo "开始切换..."
hdfs haadmin -failover "${ACTIVE}" "${STANDBY}"

if [ $? -eq 0 ]; then
    echo ""
    echo "切换成功!"
    sleep 3
    NEW_ACTIVE=$(get_active_nn)
    NEW_STANDBY=$(get_standby_nn)
    echo "新状态:"
    echo "  Active: ${NEW_ACTIVE}"
    echo "  Standby: ${NEW_STANDBY}"
else
    echo ""
    echo "切换失败!"
    exit 1
fi
