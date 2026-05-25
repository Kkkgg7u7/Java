# HDFS HA 生产环境部署检查清单

## 部署前检查

- [ ] 服务器硬件资源符合要求（CPU、内存、磁盘）
- [ ] 3台服务器配置完成（node1, node2, node3）
- [ ] Java 8+ 已安装并配置 JAVA_HOME
- [ ] ZooKeeper 3节点集群已部署并运行
- [ ] 所有节点之间 SSH 免密登录已配置
- [ ] 主机名解析正常（/etc/hosts 或 DNS）
- [ ] 防火墙已开放所需端口
- [ ] 磁盘分区已完成，有足够空间
- [ ] NTP 时间同步已配置

## 配置检查

- [ ] core-site.xml 已配置并复制到所有节点
- [ ] hdfs-site.xml 已配置并复制到所有节点
- [ ] hadoop-env.sh 已配置并复制到所有节点
- [ ] workers 文件已配置并复制到所有节点
- [ ] hadoop-metrics2.properties 已配置（监控用）
- [ ] log4j2.properties 已配置
- [ ] 路径配置符合实际环境（/opt/hadoop）
- [ ] ZooKeeper 连接地址正确
- [ ] NameNode RPC/HTTP 地址正确
- [ ] JournalNode 地址配置正确

## 部署执行检查

- [ ] JournalNode 已启动（3个节点）
- [ ] nn1 已格式化并启动
- [ ] ZKFC 已格式化
- [ ] nn2 已执行 bootstrapStandby 并启动
- [ ] ZKFC 已启动（2个节点）
- [ ] DataNode 已启动（3个节点）
- [ ] 一个 NameNode 处于 Active 状态
- [ ] 另一个 NameNode 处于 Standby 状态
- [ ] HDFS Web UI 可以访问
- [ ] 可以执行 hdfs dfs 命令

## 功能验证检查

- [ ] 可以创建目录：hdfs dfs -mkdir /test
- [ ] 可以上传文件：hdfs dfs -put test.txt /test
- [ ] 可以下载文件：hdfs dfs -get /test/test.txt
- [ ] 可以删除文件：hdfs dfs -rm /test/test.txt
- [ ] 文件副本数正确（3副本）
- [ ] 手动故障转移成功
- [ ] 自动故障转移模拟测试成功
- [ ] 读写操作在故障转移后正常

## 监控检查

- [ ] NameNode JVM 指标可采集
- [ ] DataNode JVM 指标可采集
- [ ] JournalNode 指标可采集
- [ ] ZKFC 指标可采集
- [ ] Prometheus 配置已部署
- [ ] Grafana 仪表板已导入
- [ ] 告警规则已配置
- [ ] 告警通知渠道已配置

## 备份检查

- [ ] 备份脚本已部署
- [ ] 备份目标目录已创建
- [ ] 备份测试执行成功
- [ ] 备份文件完整性验证
- [ ] 备份保留策略已配置
- [ ] 定期备份任务已配置（crontab）
- [ ] 备份恢复流程已验证

## 安全检查（可选，推荐）

- [ ] SSL/TLS 证书已生成
- [ ] ssl-site.xml 已配置
- [ ] 数据传输加密已启用
- [ ] Kerberos 认证已配置（如果需要）
- [ ] HDFS 权限已配置
- [ ] 防火墙规则已严格配置
- [ ] 访问日志已启用

## 运维文档检查

- [ ] 运维手册已熟悉
- [ ] 故障处理手册已熟悉
- [ ] 团队已接受培训
- [ ] 联系人列表已准备
- [ ] 应急响应流程已确定

## 上线前最终检查

- [ ] 所有检查项已完成
- [ ] 性能基准测试已完成
- [ ] 容量规划已完成
- [ ] 灾备方案已就绪
- [ ] 回滚方案已准备
- [ ] 业务部门已确认
- [ ] 变更窗口已批准

---

**签审记录：**

部署工程师：___________ 日期：___________

运维负责人：___________ 日期：___________

技术负责人：___________ 日期：___________
