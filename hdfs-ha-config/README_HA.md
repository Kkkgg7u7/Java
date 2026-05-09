# HDFS 高可用集群企业级配置

## 集群架构

```
                    ┌─────────────┐
                    │   ZooKeeper │
                    │   Quorum    │
                    │ (3 节点)    │
                    └──────┬──────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │  ZKFC   │       │  ZKFC   │       │         │
   ├─────────┤       ├─────────┤       │         │
   │ NameNode│       │ NameNode│       │Journal..│
   │  (nn1)  │◄─────►│  (nn2)  │◄─────►│Cluster  │
   │ Active  │       │ Standby │       │ (3 JN)  │
   └────┬────┘       └────┬────┘       └────┬────┘
        │                 │                 │
        └─────────────────┼─────────────────┘
                          │
                    ┌─────▼─────┐
                    │  DataNode │
                    │  Cluster  │
                    └───────────┘
```

## 目录结构

```
d:\Projects\Git\Mind\509\
├── etc/hadoop/                    # 配置文件目录
│   ├── core-site.xml              # 核心配置
│   ├── hdfs-site.xml              # HDFS HA 配置
│   ├── hadoop-env.sh              # 环境变量
│   ├── workers                    # DataNode 列表
│   ├── ssl-site.xml               # SSL/TLS 配置
│   ├── hadoop-policy.xml          # 安全策略
│   ├── hdfs-metrics.properties    # 监控指标配置
│   └── log4j2.properties          # 日志配置
├── scripts/                       # 运维脚本
│   ├── monitor.sh                 # 集群监控
│   ├── manual-failover.sh         # 手动故障转移
│   ├── checkpoint.sh              # 手动检查点
│   └── rolling-restart-dn.sh      # 滚动重启 DataNode
├── docs/                          # 文档
│   ├── TROUBLESHOOTING.md         # 故障处理指南
│   └── OPERATIONS.md              # 运维手册
├── deploy-ha.sh                   # 部署脚本
├── test-failover.sh               # 故障转移测试
└── stop-cluster.sh                # 停止集群
```

## 快速开始

### 前置要求

- 3 台服务器 (node1, node2, node3)
- Java 8+ 已安装
- ZooKeeper 集群已部署并运行
- SSH 免密登录已配置
- 主机名解析正确配置

### 部署步骤

1. **复制配置文件到所有节点:**
```bash
# 在所有节点上创建目录
for node in node1 node2 node3; do
    ssh $node "mkdir -p /opt/hadoop/etc/hadoop"
    scp etc/hadoop/* $node:/opt/hadoop/etc/hadoop/
done
```

2. **修改环境变量 (可选):**

编辑 `etc/hadoop/hadoop-env.sh` 中的路径配置

3. **运行部署脚本:**
```bash
bash deploy-ha.sh
```

4. **执行故障转移测试:**
```bash
bash test-failover.sh
```

## 核心配置参数说明

### core-site.xml 关键参数

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `fs.defaultFS` | 默认文件系统 URI | hdfs://mycluster |
| `ha.zookeeper.quorum` | ZooKeeper 连接地址 | node1:2181,... |
| `ha.zookeeper.session-timeout.ms` | ZK 会话超时 | 10000 |
| `io.file.buffer.size` | IO 缓冲区大小 | 131072 |
| `fs.trash.interval` | 回收站保留时间(分钟) | 1440 |

### hdfs-site.xml 关键参数

| 参数 | 说明 |
|------|------|
| `dfs.nameservices` | 命名服务名称 |
| `dfs.ha.namenodes.mycluster` | NameNode 列表 |
| `dfs.namenode.shared.edits.dir` | JournalNode 共享编辑日志 |
| `dfs.ha.automatic-failover.enabled` | 启用自动故障转移 |
| `dfs.ha.fencing.methods` | 隔离方法 |
| `dfs.namenode.handler.count` | NameNode 处理线程数 |
| `dfs.datanode.max.transfer.threads` | DataNode 传输线程数 |

## 服务端口

| 服务 | 端口 |
|------|------|
| NameNode RPC | 8020 |
| NameNode HTTP | 50070 |
| NameNode HTTPS | 50470 |
| JournalNode RPC | 8485 |
| JournalNode HTTP | 8480 |
| ZKFC | 8019 |
| DataNode | 50010, 50075 |

## 生产环境建议

1. **使用物理服务器:** NameNode 需要稳定的硬件
2. **配置 RAID 1:** NameNode 元数据目录
3. **监控告警:** 配置集群监控和告警
4. **定期备份:** 备份 NameNode 和 JournalNode 数据
5. **容量规划:** 确保至少 20% 磁盘空间余量
6. **网络隔离:** 使用独立网络用于数据传输

## 运维工具

| 脚本 | 用途 |
|------|------|
| `deploy-ha.sh` | 部署 HA 集群 |
| `test-failover.sh` | 测试主备切换 |
| `stop-cluster.sh` | 停止集群 |
| `scripts/monitor.sh` | 监控集群状态 |
| `scripts/manual-failover.sh` | 手动故障转移 |
| `scripts/checkpoint.sh` | 触发检查点 |
| `scripts/rolling-restart-dn.sh` | 滚动重启 DataNode |

## 详细文档

- [故障处理指南](docs/TROUBLESHOOTING.md)
- [运维手册](docs/OPERATIONS.md)

## 技术支持

如有问题请参考:
- 官方 Hadoop 文档: https://hadoop.apache.org/
- 查看日志文件定位问题
- 使用 `hdfs haadmin -help` 获取命令帮助
