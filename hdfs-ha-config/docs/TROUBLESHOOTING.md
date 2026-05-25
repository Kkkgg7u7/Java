# HDFS 高可用集群故障处理指南

## 目录
- [NameNode 故障处理](#namenode-故障处理)
- [JournalNode 故障处理](#journalnode-故障处理)
- [ZKFC 故障处理](#zkfc-故障处理)
- [DataNode 故障处理](#datanode-故障处理)
- [常见问题](#常见问题)

---

## NameNode 故障处理

### 场景 1: Active NameNode 不可用

**症状:**
- 无法访问 HDFS
- 监控显示 Active NameNode 未运行
- ZKFC 未触发自动故障转移

**处理步骤:**
```bash
# 1. 检查 NameNode 状态
hdfs haadmin -getServiceState nn1
hdfs haadmin -getServiceState nn2

# 2. 查看 Standby NameNode 日志
ssh node2 "tail -100 /opt/hadoop/logs/hadoop-hdfs-namenode-*.log"

# 3. 手动触发故障转移
hdfs haadmin -failover nn1 nn2

# 4. 如果故障转移失败，强制将 Standby 转为 Active
hdfs haadmin -transitionToActive --forcemanual nn2
```

### 场景 2: Standby NameNode 无法启动

**症状:**
- Standby NameNode 进程启动后立即退出
- 日志显示元数据不一致

**处理步骤:**
```bash
# 1. 从 Active NameNode 同步元数据
ssh node2 "hdfs namenode -bootstrapStandby"

# 2. 启动 Standby NameNode
ssh node2 "hdfs --daemon start namenode"

# 3. 检查状态
hdfs haadmin -getServiceState nn2
```

---

## JournalNode 故障处理

### 场景: JournalNode 节点宕机

**症状:**
- NameNode 无法写入编辑日志
- 日志显示 JournalNode 连接失败

**处理步骤:**
```bash
# 1. 重启故障的 JournalNode
ssh node3 "hdfs --daemon start journalnode"

# 2. 确认 JournalNode 状态
jps | grep JournalNode

# 3. 检查 NameNode 日志确认已恢复连接
tail -f /opt/hadoop/logs/hadoop-hdfs-namenode-*.log
```

**注意:** 只要有 > 1/2 的 JournalNode 存活，集群仍然可用。

---

## ZKFC 故障处理

### 场景: ZKFC 进程异常

**症状:**
- 自动故障转移不工作
- ZK 中没有 active 锁

**处理步骤:**
```bash
# 1. 重启 ZKFC
ssh node1 "hdfs --daemon start zkfc"
ssh node2 "hdfs --daemon start zkfc"

# 2. 检查 ZKFC 日志
ssh node1 "tail -100 /opt/hadoop/logs/hadoop-hdfs-zkfc-*.log"

# 3. 验证 ZK 中的状态
# 使用 zkCli.sh 查看 /hadoop-ha/mycluster 路径
```

---

## DataNode 故障处理

### 场景: DataNode 下线

**症状:**
- HDFS WebUI 显示 DataNode 死亡
- 块复制警告

**处理步骤:**
```bash
# 1. 检查 DataNode 状态
ssh nodeX "jps | grep DataNode"

# 2. 重启 DataNode
ssh nodeX "hdfs --daemon start datanode"

# 3. 检查块复制状态
hdfs dfsadmin -report
```

### 滚动重启 DataNode

使用提供的脚本安全重启:
```bash
bash scripts/rolling-restart-dn.sh
```

---

## 常见问题

### Q1: 两个 NameNode 都是 Standby 状态?

**A:**
```bash
# 检查 ZKFC 和 ZooKeeper 状态
# 确保 ZooKeeper 集群可用
# 手动强制指定一个为 Active
hdfs haadmin -transitionToActive --forcemanual nn1
```

### Q2: NameNode 启动卡在安全模式?

**A:**
```bash
# 查看安全模式状态
hdfs dfsadmin -safemode get

# 如果需要，手动离开安全模式
hdfs dfsadmin -safemode leave

# 检查 DataNode 注册情况
hdfs dfsadmin -report
```

### Q3: 如何备份 NameNode 元数据?

**A:**
```bash
# 触发检查点
hdfs dfsadmin -saveNamespace

# 备份以下目录
# - /opt/hadoop/namenode (在两个 NameNode 上)
# - /opt/hadoop/journal (在 JournalNode 上)
```

### Q4: 查看集群状态的常用命令?

**A:**
```bash
# NameNode 状态
hdfs haadmin -getAllServiceState

# HDFS 健康报告
hdfs dfsadmin -report

# 文件系统检查
hdfs fsck /

# 运行监控脚本
bash scripts/monitor.sh
```

---

## 紧急恢复流程

如果整个集群异常:
1. 停止所有服务
2. 检查 ZooKeeper 状态
3. 先启动 JournalNode
4. 启动一个 NameNode 并确认状态
5. 启动另一个 NameNode
6. 启动 ZKFC
7. 启动 DataNode
8. 验证 HDFS 功能
