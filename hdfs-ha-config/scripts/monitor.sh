#!/bin/bash

CLUSTER_NAME="mycluster"
NODES=("node1" "node2" "node3")
LOG_DIR="/opt/hadoop/logs"
MONITOR_LOG="${LOG_DIR}/monitor.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "${MONITOR_LOG}"
}

check_journalnode() {
    log "检查 JournalNode 状态..."
    for node in "${NODES[@]}"; do
        if ssh "${node}" "jps | grep -q JournalNode"; then
            log "✓ ${node}: JournalNode 运行正常"
        else
            log "✗ ${node}: JournalNode 未运行!"
        fi
    done
}

check_namenode() {
    log "检查 NameNode 状态..."
    for nn in nn1 nn2; do
        state=$(hdfs haadmin -getServiceState "${nn}" 2>/dev/null)
        if [ $? -eq 0 ]; then
            log "✓ ${nn}: ${state}"
        else
            log "✗ ${nn}: 获取状态失败"
        fi
    done
}

check_zkfc() {
    log "检查 ZKFC 状态..."
    for node in node1 node2; do
        if ssh "${node}" "jps | grep -q DFSZKFailoverController"; then
            log "✓ ${node}: ZKFC 运行正常"
        else
            log "✗ ${node}: ZKFC 未运行!"
        fi
    done
}

check_datanode() {
    log "检查 DataNode 状态..."
    count=0
    for node in "${NODES[@]}"; do
        if ssh "${node}" "jps | grep -q DataNode"; then
            log "✓ ${node}: DataNode 运行正常"
            ((count++))
        else
            log "✗ ${node}: DataNode 未运行!"
        fi
    done
    log "DataNode 总数: ${count}"
}

check_hdfs_health() {
    log "检查 HDFS 健康状态..."
    if hdfs dfsadmin -report &>/dev/null; then
        safe_mode=$(hdfs dfsadmin -safemode get | awk '{print $NF}')
        log "✓ 安全模式: ${safe_mode}"
        live_nodes=$(hdfs dfsadmin -report | grep "Live datanodes" | awk '{print $3}' | tr -d '()')
        log "✓ 存活 DataNode: ${live_nodes}"
    else
        log "✗ HDFS 不可用!"
    fi
}

check_disk_usage() {
    log "检查磁盘使用情况..."
    for node in "${NODES[@]}"; do
        ssh "${node}" "df -h /opt/hadoop" | tail -1 | while read usage; do
            log "✓ ${node}: ${usage}"
        done
    done
}

echo "======================================"
echo "HDFS 高可用集群监控"
echo "======================================"

mkdir -p "${LOG_DIR}"
check_journalnode
check_namenode
check_zkfc
check_datanode
check_hdfs_health
check_disk_usage

log "监控检查完成"
echo ""
