<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge Relayer</h1>
  <p align="center">
    <a href="http://makeapullrequest.com">
      <img alt="pull requests welcome badge" src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat">
    </a>
    <a href="https://www.java.com">
      <img alt="Language" src="https://img.shields.io/badge/Language-Java-blue.svg?style=flat">
    </a>
    <a href="https://github.com/AntChainOpenLab/AntChainBridgeRelayer/graphs/contributors">
      <img alt="GitHub contributors" src="https://img.shields.io/github/contributors/AntChainOpenLab/AntChainBridgeRelayer">
    </a>
    <a href="https://www.apache.org/licenses/LICENSE-2.0">
      <img alt="License" src="https://img.shields.io/github/license/AntChainOpenLab/AntChainBridgeRelayer?style=flat">
    </a>
  </p>
</div>

# 介绍

蚂蚁链跨链桥中继（AntChain Bridge Relayer, ACB Relayer）是蚂蚁链跨链开源项目的重要组件，负责连接区块链、区块链域名服务（BCDNS）和证明转化组件（PTC），完成可信信息的流转与证明，实现区块链互操作。

ACB Relayer是从蚂蚁链跨链产品[ODATS](https://antdigital.com/products/odats)中开源出来的组件，并按照[IEEE 3205](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/ieee/p3205/IEEE_3205-2023_Final.pdf)对跨链逻辑进行了升级，目前开源版本已经支持协议中通信相关的基本流程，包括统一跨链数据包（Unified Crosschain Packet, UCP）、可认证消息（Authentic Message, AM）、智能合约数据报（Smartcontract Datagram Protocol, SDP）等消息的处理，以及基于BCDNS的实现了区块链身份管理流程和区块链之间的消息寻址功能，目前支持中国信息通信研究院基于星火链开发的BCDNS服务。

ACB Relayer将功能实现分为两部分，分别为通信和可信，目前ACB Relayer已经实现区块链合约之间的通信功能，在2024年将实现基于PTC和BCDNS的可信链路，最终提供灵活可靠的区块链互操作能力。



# 快速开始

**在开始之前，请您确保安装了maven和JDK，这里推荐使用[openjdk-1.8](https://adoptium.net/zh-CN/temurin/releases/?version=8)版本*

## 编译

*由于AntChainBridgePluginSDK没有发布到maven仓库，因此可以参考[文档](https://github.com/AntChainOpenLabs/AntChainBridgePluginSDK?tab=readme-ov-file#%E5%AE%89%E8%A3%85)本地安装，或者直接clone对应Relayer依赖版本的源码，版本可[见](./pom.xml)的标签`<acb-sdk.version>`*

在Relayer项目根目录运行maven命令即可编译：

```
mvn package -Dmaven.test.skip=true
```

在`r-bootstrap/target`下面会产生一个压缩包`acb-relayer-x.y.z.tar.gz`，将该压缩包解压到运行环境即可。

解压之后可以看到以下文件：

```
tree .
.
├── README.md
├── bin
│   ├── acb-relayer.service
│   ├── init_tls_certs.sh
│   ├── print.sh
│   ├── start.sh
│   └── stop.sh
├── config
│   ├── application.yml
│   └── db
│       └── ddl.sql
└── lib
    └── r-bootstrap-0.1.0.jar

4 directories, 9 files
```



## 环境

ACB Relayer使用了MySQL和Redis，这里建议使用docker快速安装依赖。

首先通过脚本安装docker，或者在[官网](https://docs.docker.com/get-docker/)下载。

```
wget -qO- https://get.docker.com/ | bash
```

然后下载MySQL镜像并启动容器：

```
docker run -itd --name mysql-test -p 3306:3306 -e MYSQL_ROOT_PASSWORD='YOUR_PWD' mysql --default-authentication-plugin=mysql_native_password
```

然后下载Redis镜像并启动容器：

```
docker run -itd --name redis-test -p 6379:6379 redis --requirepass 'YOUR_PWD' --maxmemory 500MB
```



## 配置

### TLS

首先，初始化中继的TLS证书，会在`tls_certs`路径下生成`relayer.crt`和`relayer.key`。

```
bin/init_tls_certs.sh 
    ___            __   ______ __            _           ____         _      __
   /   |   ____   / /_ / ____// /_   ____ _ (_)____     / __ ) _____ (_)____/ /____ _ ___
  / /| |  / __ \ / __// /    / __ \ / __ `// // __ \   / __  |/ ___// // __  // __ `// _ \
 / ___ | / / / // /_ / /___ / / / // /_/ // // / / /  / /_/ // /   / // /_/ // /_/ //  __/
/_/  |_|/_/ /_/ \__/ \____//_/ /_/ \__,_//_//_/ /_/  /_____//_/   /_/ \__,_/ \__, / \___/
                                                                            /____/        

[ INFO ]_[ 2023-12-25 20:32:17.170 ] : generate relayer.key successfully
[ INFO ]_[ 2023-12-25 20:32:17.170 ] : generate relayer.crt successfully
```

### 中间件

然后，找到`config/application.yml`，配置MySQL和Redis信息到配置文件：

```yaml
spring:
  application:
    name: antchain-bridge-relayer
  profiles:
    active: env
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/relayer?serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true
    password: YOUR_PWD
    username: root
  redis:
    host: localhost
    port: 6379
    password: YOUR_PWD
```

### 跨链身份

最后，需要向BCDNS服务申请中继身份证书，用于与BCDNS和其他中继进行交互，这里推荐搭建信通院基于星火链实现的[BCDNS]()服务，目前没有提供测试网服务，需要开发者自行运行该服务。

使用CLI工具（请参考CLI[文档]()）为中继生成私钥、公钥。

```
    ___    ______ ____     ____   ______ __     ___ __  __ ______ ____
   /   |  / ____// __ )   / __ \ / ____// /    /   |\ \/ // ____// __ \
  / /| | / /    / __  |  / /_/ // __/  / /    / /| | \  // __/  / /_/ /
 / ___ |/ /___ / /_/ /  / _, _// /___ / /___ / ___ | / // /___ / _, _/
/_/  |_|\____//_____/  /_/ |_|/_____//_____//_/  |_|/_//_____//_/ |_|

                             CLI 0.1.0

relayer:> generate-relayer-account 
private key path: /path/to/private_key.pem
public key path: /path/to/public_key.pem
```

然后，生成BID Document，用于向BCDNS申请中继证书，具体申请操作请参考BCDNS操作文档。

```
relayer:> generate-bid-document --publicKeyPath /path/to/public_key.pem
file is : /path/to/bid_document.json
```

如果仅需要将程序运行起来，或者进行某些测试，可以使用测试用例中提供的[证书](r-bootstrap/src/test/resources/cc_certs/relayer.crt)和[密钥](r-bootstrap/src/test/resources/cc_certs/private_key.pem)，请不要将该证书与密钥用于生产。

在获得中继证书和密钥之后，将其配置到文件中，这里假设将证书和密钥分别放在`cc_certs/relayer.crt`和`cc_certs/private_key.pem：`

```
relayer:
  network:
    node:
      sig_algo: Ed25519
      crosschain_cert_path: file:cc_certs/relayer.crt
      private_key_path: file:cc_certs/private_key.pem
```



## 运行

通过运行`bin/start.sh -h`，可以看到运行方式。

- 可以直接运行`start.sh`启动服务进程。

- 可以通过运行`start.sh -s`作为系统服务启动，支持自动重启等功能。

```
bin/start.sh -h

 start.sh - Start the ACB Relayer

 Usage:
   start.sh <params>

 Examples:
  1. start in system service mode：
   start.sh -s
  2. start in application mode:
   start.sh

 Options:
   -s         run in system service mode.
   -h         print help information.
```

成功运行之后，可以在`log/antchain-bridge-relayer`看到日志文件。



## 命令行交互工具（CLI）

ACB Relayer提供了一个命令行交互工具，详情请见使用[文档]()。



# 社区治理

AntChain Bridge 欢迎您以任何形式参与社区建设。

您可以通过以下方式参与社区讨论

- 钉钉

![scan dingding](https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/dingding.png)

- 邮件

发送邮件到`antchainbridge@service.alipay.com`

# License

详情参考[LICENSE](./LICENSE)。