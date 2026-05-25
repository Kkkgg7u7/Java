# HDFS Insight Lab：Spring Boot + Hadoop HDFS 数据湖实验平台

本项目是面向课程大作业展示的 Spring Boot Web 应用。系统以“业务流水”为样本数据，重点演示 Spring Boot 后端通过 Hadoop Client 调用 HDFS/HA HDFS，实现文件管理、数据预览、目录分析和读写性能实验。

项目不是普通记账系统，而是用账本流水构造一个容易讲清楚的大数据业务场景：

- Spring Boot + Hadoop FileSystem API：连接检测、目录创建、文件上传、下载、删除、批量删除。
- HDFS 数据湖实验台：展示文件系统 URI、实验目录、探测耗时、HA 客户端配置和实验结果。
- 业务流水性能实验：生成模拟 CSV 流水，写入 Hadoop FileSystem 后再读取，展示写入吞吐、读取吞吐、记录处理速率、总耗时和输出目录。
- 业务样本管理：保留账户样本、业务流水、分类和统计页面，用于说明大数据文件来自一个具体业务场景。
- HDFS HA 配置：仓库根目录 `hdfs-ha-config` 提供 `core-site.xml`、`hdfs-site.xml`、部署脚本和运维文档。

## 运行环境

- Java 11+
- Maven 3.6+
- 可选：MySQL 8
- 可选：Hadoop 3.x HA 集群

Web 应用不需要部署在 Hadoop 节点内。只要运行机器能够访问 NameNode/ZooKeeper/JournalNode 所需网络，并且本机有 HA 客户端配置 XML，即可通过 Hadoop Client 访问远程 HA HDFS。

## 默认本地演示

默认 profile 是 `demo`，使用 H2 文件数据库和 `file:///` Hadoop FileSystem，适合在没有 Hadoop 集群的机器上完整展示页面和性能流程。

```bash
cd Project2
mvn spring-boot:run
```

访问：

- HDFS 数据湖实验台：http://localhost:8080/bigdata
- 样本概览：http://localhost:8080/index
- H2 控制台：http://localhost:8080/h2-console

默认账号仅用于 demo profile 课堂演示。登录页不会明文提示或预填账号密码：

- 用户名：`admin`
- 密码：`admin`

## 推荐课堂演示流程

1. 打开 `http://localhost:8080`，登录后自动进入 `HDFS 数据湖实验台`。
2. 展示顶部状态卡：文件系统 URI、实验目录、探测耗时。
3. 在“性能实验”中选择参数预设，例如“快速实验”或“标准实验”。
4. 点击“运行实验”，系统会生成模拟业务流水 CSV，写入 Hadoop FileSystem 后再读取。
5. 展示实验结果：写入吞吐、读取吞吐、记录处理速率、总耗时、数据量、输出目录。
6. 点击“查看生成文件”，切换到 HDFS 文件列表，展示本次生成的 CSV 文件。
7. 点击“预览”，展示 HDFS 中真实存储的业务流水内容。
8. 切到“目录分析”，展示文件数量、目录数量、Top 大文件和文件类型分布。
9. 最后展示“HDFS / HA 客户端配置”，说明如果切换到 `hdfs://mycluster`，客户端会通过 Hadoop HA 配置访问集群。

## 连接远程 HA HDFS 演示

在客户端机器上准备 Hadoop HA 配置文件。当前仓库已经提供：

```text
../hdfs-ha-config/etc/hadoop/core-site.xml
../hdfs-ha-config/etc/hadoop/hdfs-site.xml
```

启动时启用 `hadoop` profile，并指定 HA 配置目录：

```bash
export HADOOP_CONF_DIR=../hdfs-ha-config/etc/hadoop
export HADOOP_DEFAULT_FS=hdfs://mycluster
export HADOOP_USER_NAME=hdfs
export HADOOP_TEST_DIR=/account-book/performance

mvn spring-boot:run -Dspring-boot.run.profiles=demo,hadoop
```

如果使用 MySQL 作为业务数据库：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql,hadoop
```

注意事项：

- 客户端需要能解析 `core-site.xml` / `hdfs-site.xml` 中的 `node1`、`node2`、`node3` 主机名。
- 如果没有 DNS，可在客户端 `/etc/hosts` 配置 Hadoop 节点 IP。
- 需要确保客户端到 NameNode RPC 端口、ZooKeeper 端口等网络可达。
- Web 应用使用 Hadoop Client 的 HA 逻辑名 `hdfs://mycluster`，故障转移由 Hadoop 客户端代理完成。

## MySQL 演示

默认 MySQL 配置在 `src/main/resources/application-mysql.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/account_book?createDatabaseIfNotExist=true...
    username: root
    password: root
```

启动：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

系统会使用 `src/main/resources/db/schema.sql` 和 `src/main/resources/db/data.sql` 初始化表和演示数据。

## 功能入口

- `/bigdata`：HDFS 数据湖实验台，展示 Spring Boot + Hadoop HDFS 的核心能力。
- `/index`：业务流水样本概览，展示样本余额、收入、支出和图表。
- `/records`：业务流水管理，支持新增、编辑、删除、筛选。
- `/accounts`：账户样本管理，支持新增、编辑、删除。
- `/stats`：业务流水分析，支持全部、本月、指定年月统计。

## Hadoop API

基础路径同时支持 `/hdfs` 与 `/api/hdfs`。这些接口需要登录后访问，并默认限制在 `hadoop.hdfs.test-dir` 实验目录内，避免课堂演示时误操作其他路径：

- `GET /api/hdfs/health`：查看 Hadoop 客户端连接状态和 HA 配置。
- `GET /api/hdfs/list?path=/account-book/performance&recursive=true`：列出文件。
- `POST /api/hdfs/mkdir`：创建目录，参数 `path`。
- `POST /api/hdfs/upload`：上传文件，参数 `path`、`file`、`overwrite`。
- `GET /api/hdfs/download?path=...`：下载文件。
- `POST /api/hdfs/delete`：删除路径，参数 `path`、`recursive`。
- `POST /api/hdfs/batch-delete`：批量删除文件或目录。
- `GET /api/hdfs/size-statistics`：统计文件数量、目录数量、总大小和 Top 大文件。
- `GET /api/hdfs/directory-analysis`：分析目录结构和文件类型分布。
- `GET /api/hdfs/data-preview`：预览 CSV/文本文件前若干行。
- `POST /api/hdfs/performance`：生成模拟业务流水并测试写入/读取性能。

性能实验参数：

- `files`：生成文件数，默认 4，最大 32。
- `records`：生成记录数，默认 20000，最大 2000000。
- `payloadSize`：每条记录填充字节，默认 128，最大 4096。
- `clean`：测试后是否删除结果目录，默认 false。

示例：

```bash
curl -X POST http://localhost:8080/api/hdfs/performance \
  -d files=4 \
  -d records=100000 \
  -d payloadSize=256 \
  -d clean=false
```

返回指标包括：

- 写入吞吐 `writeMbPerSecond`
- 读取吞吐 `readMbPerSecond`
- 记录处理速率 `recordsPerSecond`
- 总耗时 `totalMillis`
- 数据量 `bytes`
- 文件数和记录数
- 输出目录 `path`

## 安全与演示稳定性

- 登录页不再预填账号密码，也不显示默认账号提示。
- 用户密码使用 BCrypt 存储；旧 MD5 数据在登录成功后会自动升级为 BCrypt。
- HDFS API 需要登录后访问。
- HDFS 文件操作限制在 `hadoop.hdfs.test-dir` 实验目录内。
- 删除接口禁止直接删除实验根目录，降低课堂现场误操作风险。
- 上传限制：单文件最大 `64MB`，单次请求最大 `128MB`。
- 前端 ECharts 加载失败时，实验结果卡片仍可展示关键指标。

## HDFS HA 配置说明

仓库根目录 `hdfs-ha-config` 包含完整 HA 配置示例：

- `core-site.xml`：`fs.defaultFS=hdfs://mycluster`、ZooKeeper quorum、客户端超时等。
- `hdfs-site.xml`：nameservice、双 NameNode、JournalNode、自动故障转移、客户端 failover proxy。
- `deploy-ha.sh`：集群初始化部署脚本。
- `test-failover.sh`：故障转移测试脚本。
- `docs/OPERATIONS.md`、`docs/TROUBLESHOOTING.md`：运维说明。

Spring Boot 客户端启动时会读取 `HADOOP_CONF_DIR` 指向目录中的 `core-site.xml` 和 `hdfs-site.xml`，并使用其中的 HA 逻辑名与 failover 配置。

## 项目结构

```text
Project2
├── pom.xml
├── sql/init.sql
├── src/main/java/com/account
│   ├── config          # Hadoop 配置绑定和异常处理
│   ├── controller      # 页面与 REST 接口
│   ├── dao             # MyBatis Mapper
│   ├── dto             # HDFS 演示返回对象
│   ├── entity          # 用户、账户、分类、流水记录实体
│   ├── service         # 业务服务接口
│   └── util            # 结果封装、HDFS 客户端、密码/日期/金额工具
└── src/main/resources
    ├── application.yml
    ├── application-demo.yml
    ├── application-mysql.yml
    ├── application-hadoop.yml
    ├── db              # H2/MySQL 初始化脚本
    ├── mapper          # MyBatis XML
    ├── static          # 公共 CSS 和页面 JS
    └── templates       # Thymeleaf 页面
```
