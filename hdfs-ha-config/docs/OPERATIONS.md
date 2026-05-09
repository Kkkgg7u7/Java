# HDFS 高可用集群运维手册

## 日常运维任务

### 1. 集群状态检查

**每日检查清单:**
```bash
# 运行监控脚本
bash scripts/monitor.sh

# 检查 NameNode 状态
hdfs haadmin -getAllServiceState

# 检查 HDFS 健康
hdfs fsck / -files -blocks -locations
```

### 2. 日志查看

关键日志位置:
- NameNode: `/opt/hadoop/logs/hadoop-hdfs-namenode-*.log`
- DataNode: `/opt/hadoop/logs/hadoop-hdfs-datanode-*.log`
- JournalNode: `/opt/hadoop/logs/hadoop-hdfs-journalnode-*.log`
- ZKFC: `/opt/hadoop/logs/hadoop-hdfs-zkfc-*.log`

查看实时日志:
```bash
tail -f /opt/hadoop/logs/hadoop-hdfs-namenode-$(hostname).log
```

### 3. 元数据备份

**定期备份操作:**
```bash
# 1. 触发检查点
bash scripts/checkpoint.sh

# 2. 备份 NameNode 元数据目录
# 在 nn1 和 nn2 上执行:
tar -czf namenode-backup-$(date +%Y%m%d).tar.gz /opt/hadoop/namenode

# 3. 备份 JournalNode 编辑日志
# 在 JournalNode 上执行:
tar -czf journal-backup-$(date +%Y%m%d).tar.gz /opt/hadoop/journal
```

## 扩容操作

### 添加新 DataNode

1. 在新节点上部署 Hadoop 配置
2. 配置 SSH 免密登录
3. 更新 `workers` 文件
4. 在新节点启动 DataNode:
```bash
ssh newnode "hdfs --daemon start datanode"
```
5. 验证 DataNode 已注册:
```bash
hdfs dfsadmin -report
```
6. 可选: 触发数据平衡
```bash
hdfs balancer
```

### 数据平衡

```bash
# 启动平衡器
hdfs balancer -threshold 10

# 后台运行
nohup hdfs balancer -threshold 10 > balancer.log 2>&1 &
```

## 配置变更

### 修改配置后重启服务

**NameNode 配置变更 (需要滚动重启):**
1. 在 Standby NameNode 修改配置
2. 重启 Standby NameNode
3. 故障转移到更新后的 NameNode
4. 在另一个 NameNode 更新配置并重启

**JournalNode 配置变更:**
- 建议一次只重启一个 JournalNode
- 确保大多数 JournalNode 始终可用

**DataNode 配置变更:**
- 使用滚动重启脚本
- `bash scripts/rolling-restart-dn.sh`

## 性能优化

### 监控关键指标

```bash
# 查看 NameNode JVM 统计
# 通过 JMX 或 WebUI

# 查看 DataNode 磁盘 IO
iostat -x 5

# 查看网络流量
iftop
```

### 常用性能参数调整

编辑 `hdfs-site.xml`:
- `dfs.datanode.max.transfer.threads`: 增加并发传输数
- `dfs.datanode.balance.bandwidthPerSec`: 调整平衡带宽
- `dfs.namenode.handler.count`: 调整 NameNode 处理线程数

## 安全建议

1. 定期更新 Hadoop 版本修复安全漏洞
2. 配置防火墙限制访问端口
3. 启用 Kerberos 认证 (生产环境)
4. 定期审查 HDFS 权限设置
5. 监控异常访问日志

## Web UI 访问

- Active NameNode: http://node1:50070
- Standby NameNode: http://node2:50070
- JournalNode: http://nodeX:8480
