# HDFS HA Kerberos 安全配置指南

## 前置条件

- 已配置并运行的 KDC (Key Distribution Center)
- 所有节点已加入 Kerberos 域
- 具有 KDC 管理员权限

## 配置步骤

### 1. 创建 Kerberos Principal

在 KDC 服务器上执行：

```bash
kadmin.local

# 创建 NameNode principals
addprinc -randkey nn/node1@EXAMPLE.COM
addprinc -randkey nn/node2@EXAMPLE.COM

# 创建 DataNode principals
addprinc -randkey dn/node1@EXAMPLE.COM
addprinc -randkey dn/node2@EXAMPLE.COM
addprinc -randkey dn/node3@EXAMPLE.COM

# 创建 JournalNode principals
addprinc -randkey jn/node1@EXAMPLE.COM
addprinc -randkey jn/node2@EXAMPLE.COM
addprinc -randkey jn/node3@EXAMPLE.COM

# 创建 HTTP principals (for web UI)
addprinc -randkey HTTP/node1@EXAMPLE.COM
addprinc -randkey HTTP/node2@EXAMPLE.COM
addprinc -randkey HTTP/node3@EXAMPLE.COM

# 创建 hdfs 超级用户
addprinc -randkey hdfs@EXAMPLE.COM
```

### 2. 创建并分发 Keytab 文件

```bash
# 在 KDC 服务器上为每个节点创建 keytab
kadmin.local -q "xst -k nn.service.keytab nn/node1@EXAMPLE.COM HTTP/node1@EXAMPLE.COM"
kadmin.local -q "xst -k nn.service.keytab nn/node2@EXAMPLE.COM HTTP/node2@EXAMPLE.COM"

kadmin.local -q "xst -k dn.service.keytab dn/node1@EXAMPLE.COM"
kadmin.local -q "xst -k dn.service.keytab dn/node2@EXAMPLE.COM"
kadmin.local -q "xst -k dn.service.keytab dn/node3@EXAMPLE.COM"

kadmin.local -q "xst -k jn.service.keytab jn/node1@EXAMPLE.COM"
kadmin.local -q "xst -k jn.service.keytab jn/node2@EXAMPLE.COM"
kadmin.local -q "xst -k jn.service.keytab jn/node3@EXAMPLE.COM"

kadmin.local -q "xst -k hdfs.keytab hdfs@EXAMPLE.COM"
```

分发 keytab 文件到各节点：

```bash
# 确保权限正确
chmod 600 *.keytab
chown hdfs:hadoop *.keytab

# 复制到各节点
scp nn.service.keytab node1:/etc/security/keytabs/
scp nn.service.keytab node2:/etc/security/keytabs/

scp dn.service.keytab node1:/etc/security/keytabs/
scp dn.service.keytab node2:/etc/security/keytabs/
scp dn.service.keytab node3:/etc/security/keytabs/

scp jn.service.keytab node1:/etc/security/keytabs/
scp jn.service.keytab node2:/etc/security/keytabs/
scp jn.service.keytab node3:/etc/security/keytabs/
```

### 3. 部署 Kerberos 配置文件

```bash
# 备份原配置
cp etc/hadoop/core-site.xml etc/hadoop/core-site.xml.backup

# 使用 Kerberos 配置
cp etc/hadoop/kerberos/core-site.xml.kerberos etc/hadoop/core-site.xml
```

编辑配置文件中的：
- `EXAMPLE.COM` 替换为实际的 Kerberos 域名
- 路径配置根据实际环境调整

### 4. 配置 DataNode 安全访问

在 `hdfs-site.xml` 中添加：

```xml
<property>
  <name>dfs.datanode.address</name>
  <value>0.0.0.0:1004</value>
</property>
<property>
  <name>dfs.datanode.http.address</name>
  <value>0.0.0.0:1006</value>
</property>
<property>
  <name>dfs.datanode.data.dir.perm</name>
  <value>700</value>
</property>
```

**注意：** 端口 1004 和 1006 需要 root 权限启动 DataNode，或使用 JSVC。

### 5. 重启 HDFS 服务

```bash
# 停止所有服务
./stop-cluster.sh

# 重新启动
./deploy-ha.sh
```

### 6. 验证 Kerberos 认证

```bash
# 使用 hdfs 用户认证
kinit -kt /etc/security/keytabs/hdfs.keytab hdfs@EXAMPLE.COM

# 测试 HDFS 访问
hdfs dfs -ls /
```

## 故障排查

### 常见错误

**1. 没有权限访问 HDFS**
- 检查是否正确执行了 kinit
- 检查 principal 是否正确

**2. DataNode 无法启动**
- 检查 keytab 文件权限
- 检查端口权限（1004/1006）
- 查看 DataNode 日志

**3. NameNode 无法与 JournalNode 通信**
- 检查所有节点的时间同步
- 验证 JournalNode 的 principal
- 查看网络连接和防火墙

### 有用命令

```bash
# 查看当前 ticket
klist

# 销毁 ticket
kdestroy

# 测试 keytab
kinit -kt /path/to/keytab principal@REALM

# 查看 HDFS 安全状态
hdfs dfsadmin -report
```
