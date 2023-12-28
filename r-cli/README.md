<div align="center">
  <img alt="am logo" src="https://antchainbridge.oss-cn-shanghai.aliyuncs.com/antchainbridge/document/picture/antchain.png" width="250" >
  <h1 align="center">AntChain Bridge Relayer CLI</h1>
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

中继CLI工具是用于管理中继的交互式命令行工具，它可以完成区块链授权、注册、停止服务等工作。
​

# 使用

## 编译

在r-cli模块根目录运行maven命令即可：

```shell
cd r-cli && mvn package -Dmaven.test.skip=true
```

在`r-cli/target`目录下会生成一个压缩包`r-cli-bin.tar.gz`，解压改压缩包即可使用。

解压编译生成包后可以看到文件如下：
```shell
target/r-cli
├── README.md
├── bin
│   └── start.sh
└── lib
    └── r-cli.jar

3 directories, 3 files
```

## 启动

执行编译生成包中的执行脚本即可启动中继CLI工具，命令执行情况如下：

```shell
$ ./r-cli/bin/start.sh

    ___    ______ ____     ____   ______ __     ___ __  __ ______ ____
   /   |  / ____// __ )   / __ \ / ____// /    /   |\ \/ // ____// __ \
  / /| | / /    / __  |  / /_/ // __/  / /    / /| | \  // __/  / /_/ /
 / ___ |/ /___ / /_/ /  / _, _// /___ / /___ / ___ | / // /___ / _, _/
/_/  |_|\____//_____/  /_/ |_|/_____//_____//_/  |_|/_//_____//_/ |_|

                             CLI 0.1.0

relayer:>
```

启动成功后即可在`relayer:>`启动符后执行cli命令。

# 命令操作详情

中继CLI工具目前累计支持`48`条交互命令，分别应用于BCDNS管理、区块链管理、中继器管理、服务管理及其他工具功能。

- 直接输入`help`可以查看支持命令概况
- 直接输入`version`可以查看当前中继CLI工具版本
- 直接输入`history`可以查询历史命令记录

## 1 BCDNS管理

中继CLI工具提供 BCDNS 管理相关指令，用于支持 BCDNS 的注册、停止和重启以及域名证书的申请注册等功能。

（1.1～1.6为BCDNS服务本身的管理命令，1.7～1.12为域名证书申请注册相关命令）

### 1.1 register-bcdnsservice 注册BCDNS服务

用于将指定 BCDNS 服务注册绑定到指定域名空间的中继服务，
注册成功后该 BCDNS 服务可为绑定中继提供域名管理功能（即可以进行`申请域名证书`等操作）。
该操作是中继部署成功后需要执行的操作。

命令参数如下：

- `--bcdnsType`：（必选）BCDNS 服务类型，目前提供`embedded`和`bif`两种类型。其中`embedded`为？？，`bif`为星火链网提供的BCDNS服务；
- `--domainSpace`：（可选）当前中继服务的域名空间名，一个域名空间名绑定一个 BCDNS 服务，该项默认为空字符串，即当前中继的根域名空间名默认为空字符串；
- `--propFile`：（必选）配置文件路径，即初始化 BCDSN 服务存根所需的客户端配置文件路径，例如`/path/to/your/prop.json`，该配置文件可以使用当前文档5.3节介绍的`generate-bif-bcdns-conf`命令生成；
- `--bcdnsCertPath`：（可选）BCDNS 服务的证书路径，可不提供，若未提供该证书命令执行注册时会向 BCDNS 服务请求证书。

用法如下：

```shell
relayer:> register-bcdnsservice --bcdnsType bif --propFile ./../acb-relayer1/acb-relayer/bcdns_bif1.json
？？
```

### 1.2 get-bcdnsservice 查询BCDNS服务

用于查询指定域名空间的中继所绑定的BCDNS服务。

命令参数如下：

`--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串。

用法如下：

```shell
# 当前中继域名空间名为空字符串，故直接使用默认域名空间名
relayer:> get-bcdnsservice
# 返回的服务信息中部分敏感信息已使用`***`替换
{"domainSpace":"","domainSpaceCertWrapper":{"desc":"","domainSpace":"","domainSpaceCert":{"credentialSubject":"***","credentialSubjectInstance":{"applicant":{"rawId":"ZGlkOmJpZDplZk1ka0d5S2ZtaXpYTnBYdDNTRXZKRjhnNTdtRENwQw==","type":"BID"},"bcdnsRootOwner":{"$ref":"$.domainSpaceCertWrapper.domainSpaceCert.credentialSubjectInstance.applicant"},"bcdnsRootSubjectInfo":"此处省略","name":"root_verifiable_credential","rawSubjectPublicKey":"Q3hxGTc6hCORiZqbMIDgM2YB6C0BdnCPsFpOi3cJwqA=","subject":"***","subjectPublicKey":{"algorithm":"Ed25519","encoded":"***","format":"X.509","pointEncoding":"***"}},"encodedToSign":"***","expirationDate":1733538286,"id":"did:bid:ef29QeETQp5g8wZmpKE3QeXKZ2Mzdj8ph","issuanceDate":1702002286,"issuer":{"rawId":"***","type":"BID"},"proof":{"certHash":"***","hashAlgo":"SM3","rawProof":"***","sigAlgo":"Ed25519"},"type":"BCDNS_TRUST_ROOT_CERTIFICATE","version":"1"},"ownerOid":{"rawId":"***","type":"BID"}},"ownerOid":{"rawId":"***","type":"BID"},"properties":"***","state":"WORKING","type":"BIF"}
```

### 1.3 delete-bcdnsservice 删除BCDNS服务

用于删除指定域名空间的中继所绑定的BCDNS服务，删除后可重新绑定其他 BCDNS 服务？？。

命令参数如下：

`--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串。

用法如下：
```shell
relayer:> delete-bcdnsservice
？？
```

### 1.4 get-bcdnscertificate 查询BCDNS服务证书

用于查询指定域名空间的中继所绑定的BCDNS服务的证书。

命令参数如下：

`--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串。

用法如下：

```shell
relayer:> get-bcdnscertificate
-----BEGIN BCDNS TRUST ROOT CERTIFICATE-----
# 证书具体内容省略……
-----END BCDNS TRUST ROOT CERTIFICATE-----
```

### 1.5 stop-bcdnsservice 停止BCDNS服务

用于停止指定域名空间的中继所绑定的BCDNS服务的运行。

命令参数如下：

`--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串。

用法如下：

```shell
# 停止BCDNS服务
relayer:> stop-bcdnsservice
success

# 停止后查看BCDNS服务信息可以看到，信息详情中的状态为`FROZEN`
relayer:> get-bcdnsservice
{"domainSpace":"","domainSpaceCertWrapper":{...其他信息省略...,"state":"FROZEN","type":"BIF"}
```

### 1.6 restart-bcdnsservice 重启BCDNS服务

用于重新启动指定域名空间的中继所绑定的BCDNS服务。

命令参数如下：

`--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串。

用法如下：

```shell
# 重启BCDNS服务
relayer:> restart-bcdnsservice
success

# 停止后查看BCDNS服务信息可以看到，信息详情中的状态为`WORKING`
relayer:> get-bcdnsservice
{"domainSpace":"","domainSpaceCertWrapper":{...其他信息省略...,"state":"WORKING","type":"BIF"}
```

### 1.7 apply-domain-name-cert 申请域名证书

用于为区块链管理身份申请域名证书，一条区块链可以申请多个域名证书。

命令参数如下：

`--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串；
`--domain`：（必选）待申请的域名，域名可自定义但不可重复；
`--applicantOidType`：（可选）申请域名证书的类型，目前提供`BID`和`X509_PUBLIC_KEY_INFO`两种域名证书类型，默认为`BID`类型；
`--oidFilePath`：（必选）申请域名证书需要提供的相关文件路径。申请`BID`证书需要提供包含相关信息的json格式文件，申请`X509_PUBLIC_KEY_INFO`类型证书需要提供x509公钥文件。

用法如下：

```shell
relayer:> apply-domain-name-cert --domain mychain006ly.bif --oidFilePath /data1/r-cli/bid_document.json
your receipt is f49431cf0512d5c3ae8254126a12be52
# 请将返回的`f49431cf0512d5c3ae8254126a12be52`字符串回执提供给BCDNS服务运维人员，运维人员审核申请通过后该域名生效，即可用于区块链注册
```

### 1.8 query-domain-cert-application-state 查询域名证书申请状态

用于查询域名证书的申请状态。
执行完1.7`apply-domain-name-cert`命令后可以执行该命令查询申请是否通过。

命令参数如下：

- `--domain`：（必选）区块链域名。

用法如下：

```shell
# 申请完域名立即查询申请状态，状态为申请中`applying`状态
relayer:> query-domain-cert-application-state --domain mychain006ly.bif
your application not finished: applying

# 待BCDNS服务运维人员审核通过后查询申请状态，状态为申请通过`apply_success`状态
relayer:> query-domain-cert-application-state --domain mychain006ly.bif
your application finished: apply_success

# 查询不存在的域名申请状态，提示申请不存在
relayer:> query-domain-cert-application-state --domain mychain007ly.bif
no application record found
```

### 1.9 fetch-domain-name-cert-from-bcdns 获取域名证书

用于从BCDNS服务上获取指定域名的证书并存储，一般主要用于获取其他中继服务上注册的域名证书并存储到当前中继服务。？？

命令参数如下：

- `--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串；
- `--domain`：（必选）区块链域名。

用法如下：

```shell
# 获取当前中继上申请的域名证书，由于当前中继上申请的域名证书默认已经存储，故此处会报错并提示已存在
relayer:> fetch-domain-name-cert-from-bcdns --domain mychain005ly.bif
failed to query from BCDNS: domain cert for mychain005ly.bif already exist

# 获取远程中继上申请的域名证书，打印出域名证书的信息并存储到本地中继服务
relayer:> fetch-domain-name-cert-from-bcdns --domain chain001.bif
the cert is :
-----BEGIN DOMAIN NAME CERTIFICATE-----
# 证书具体内容省略
-----END DOMAIN NAME CERTIFICATE-----

# 获取不存在的域名证书，直接报错提示当前BCDNS服务上找不到该域名的证书
relayer:> fetch-domain-name-cert-from-bcdns --domain mychain007ly.bif
none cert found for domain mychain007ly.bif on BCDNS
```

### 1.10 query-domain-name-cert-from-bcdns 查询域名证书

用于从BCDNS服务查询指定域名的域名证书，若域名已存在会打印域名证书内容，若域名证书尚未申请或尚未申请通过，会返回域名证书不存在的提示。
该命令与1.9`fetch-domain-name-cert-from-bcdns`命令的区别在于当前命令会打印出所有BCDNS服务能查到的可用证书但不做本地存储。

命令参数如下：

- `--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串；
- `--domain`：（必选）区块链域名。

用法如下：

```shell
# 查询已存在的域名证书
relayer:> query-domain-name-cert-from-bcdns --domain mychain005ly.bif
the cert is :
-----BEGIN DOMAIN NAME CERTIFICATE-----
AACoAQAAAAABAAAAMQEAKAAAAGRpZDpiaWQ6ZWZCRFN1ODhkOVRHdnJxUUJobVE5
NDdTdlRndXVMeFMCAAEAAAABAwA7AAAAAAA1AAAAAAABAAAAAQEAKAAAAGRpZDpi
aWQ6ZWZNZGtHeUtmbWl6WE5wWHQzU0V2SkY4ZzU3bURDcEMEAAgAAABv/IRlAAAA
AAUACAAAAO8vZmcAAAAABgB7AAAAAAB1AAAAAAABAAAAMQEAAQAAAAACAAAAAAAD
ABAAAABteWNoYWluMDA1bHkuYmlmBAA/AAAAAAA5AAAAAAABAAAAAAEALAAAADAq
MAUGAytlcAMhAK9mXuVQY16YN1x7OIdqLMMeOp2BiVfkaiSg8lp0pEzOBQAAAAAA
BwCIAAAAAACCAAAAAAADAAAAU00zAQAgAAAAw4oXcu08QhWKFHlw6NPaoENgdQuE
TBQwI0uqjhN5sloCAAcAAABFZDI1NTE5AwBAAAAAb07wXbL/WxFaOxmWs77Ha0P/
0i4LAAl6F/KgW2fj7Al7Q7aobbvc+GXw1SfjZ60RBSf2VMkldnSv095M9RXIAw==
-----END DOMAIN NAME CERTIFICATE-----

# 查询不存在的域名证书
relayer:> query-domain-name-cert-from-bcdns --domain mychain006ly.bif
none cert found for domain mychain006ly.bif on BCDNS
```

### 1.11 register-domain-router 注册域名路由

用于注册域名路由信息，当需要进行跨中继通信时，需要为指定域名注册域名路由，
这样BCDNS服务才能识别出指定域名属于哪个中继，进而为中继提供域名路由服务。

命令参数如下：

- `--domain`：（必选）区块链域名。

用法如下：

```shell
relayer:> register-domain-router --domain mychain004ly.bif
success
```

### 1.12 query-domain-router 查询域名路由

用于查询指定域名及域名空间的域名路由情况，命令会返回域名所属中继的信息。

参数如下：

- `--domainSpace`：（可选）中继的域名空间名，该项默认为空字符串；
- `--domain`：（必选）区块链域名。

用法如下：

```shell
relayer:> query-domain-router --domain mychain004ly.bif
{"destDomain":{"domain":"mychain004ly.bif","domainSpace":false},"destRelayer":{"netAddressList":["https://172.16.0.49:8082^https://172.16.0.50:8082"],"relayerCert":{"credentialSubject":"***","credentialSubjectInstance":{"applicant":{"rawId":"**","type":"BID"},"name":"relay","rawSubjectPublicKey":"***","subject":"***","subjectInfo":"***","subjectPublicKey":{"algorithm":"Ed25519","encoded":"***":"X.509","pointEncoding":"***"},"version":"1.0"},"encodedToSign":"***","expirationDate":1733811853,"id":"did:bid:efGeAv4Jr7V2FSyun77m4xTFmTDfG8nh","issuanceDate":1702275853,"issuer":{"rawId":"***","type":"BID"},"proof":{"certHash":"***","hashAlgo":"SM3","rawProof":"***","sigAlgo":"Ed25519"},"type":"RELAYER_CERTIFICATE","version":"1"},"relayerCertId":"did:bid:efGeAv4Jr7V2FSyun77m4xTFmTDfG8nh"}}
```

## 2 区块链管理

中继CLI工具提供区块链管理相关指令，用于支持区块链的注册、授权等功能。

### 2.1 add-blockchain-anchor 添加区块链

用于向中继添加指定区块链，需要携带区块链绑定的插件服务信息及区块链配置信息。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`，该类型的字符串应与插件服务支持类型的字符串相对应；
- `--domain`：（必选）区块链域名，应当为申请通过的区块链域名；
- `--blockchainId`：（必选）区块链标识ID，可自定义不可重复；
- `--pluginServerId`：（必选）插件服务ID，支持当前链类型且运行中的可用插件服务的标识ID，操作成功后相应区块链将基于该插件服务进行链上交互；
- `--alias`：（可选）区块链别名，可自定义，默认为空字符串；
- `--desc`：（可选）区块链描述，可自定义，默认为空字符串；
- `--confFile`：（必选）区块链配置信息路径，具体配置信息要求根据插件服务中相应链类型的插件要求。

用法如下： 

```shell
relayer:> add-blockchain-anchor --product mychain010 --domain mychain006ly.bif --blockchainId mychain006ly.id --pluginServerId testps -
-confFile /data1/acb-relayer1/acb-relayer/mychain00.json
success
```

### 2.2 deploy-bbccontracts-async 部署bbc合约

用于为指定区块链添加异步部署BBC合约的任务，任务后并不会马上执行，需要启动区块链的扫块服务后才会执行合约部署。

参数如下：

`--product`：（必选）区块链类型，如`mychain010`；
`--blockchainId`：（必选）区块链标识ID。

用法如下：

```shell
relayer:> deploy-bbccontracts-async --product mychain010 --blockchainId mychain006ly.id
success
```

### 2.3 start-blockchain-anchor 启动区块链扫块服务

用于启动指定区块链的扫块服务，执行该命令后中继会开始扫描并执行指定区块链的区块，包括执行已添加的合约部署任务。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID。

用法如下：

```shell
relayer:> start-blockchain-anchor --product mychain010 --blockchainId mychain006ly.id
success
```

### 2.4 stop-blockchain-anchor 停止区块链扫块服务

用于停止指定区块链的扫块服务，执行该命令后中继将会停止扫描指定区块链的区块，也就暂时不再支持链上合约部署交互或跨链交易等功能。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID。

用法如下：

```shell
relayer:> stop-blockchain-anchor --product mychain010 --blockchainId mychain006ly.id
success
```

### 2.5 update-blockchain-anchor 更新区块链信息

用于更新指定区块链的整个配置信息，可更新信息包括区块链别名、区块链描述及区块链配置信息。

！！！注意请勿随意使用注册配置信息作为更新配置信息，区块链启动部署合约后配置信息有所变更。
？？注册的confFile和这里的clientConfig是一个文件吗？只需要添加合约信息就可以了吗？

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID；
- `--alias`：（可选）区块链别名，可自定义，默认为空字符串；
- `--desc`：（可选）区块链描述，可自定义，默认为空字符串；
- `--clientConfig`：（必选）区块链配置信息路径，具体配置信息要求根据插件服务中相应链类型的插件要求。

用法如下：

```shell

```

### 2.6 update-blockchain-property 更新区块链配置属性

用于更新指定区块链配置信息中的具体配置属性。

！！！请勿随意修改区块链配置属性信息！！！

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID；
- `--confKey`：（必选）具体配置属性的key；
- `--confValue`：（必选）具体配置属性的value。

用法如下：

```shell
# 手动修改合约信息
relayer:> update-blockchain-property --product mychain010 --blockchainId mychain006ly.id --confKey am_client_contract_address --confVal
ue "{\"evm\":\"AM_EVM_CONTRACT_98c64d5e-9351-47f8-b1db-9f261147082c\", \"wasm\":\"AM_WASM_CONTRACT_c886302c-243a-4ae6-a890-c1aa2f69dc68\"}"
success
```

### 7. set-tx-pending-limit 设置链交易缓存容量

用于设置指定区块链接收跨链消息交易池的容量，该交易池缓存了该链所有待处理未提交的跨链交易，默认缓存容量为？？。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID；
- `--txPendingLimit`：（必选）交易缓存容量。

用法如下：

```shell
relayer:> set-tx-pending-limit --product mychain010 --blockchainId mychain006ly.id --txPendingLimit 100
success
```

### 8. get-blockchain-id-by-domain 查询区块链ID

用于根据区块链域名查询区块链ID。

参数如下：

- `--domain`：（必选）区块链域名。

用法如下：

```shell
relayer:> get-blockchain-id-by-domain --domain mychain006ly.bif
( product: mychain010 , blockchain_id: mychain006ly.id )
```

### 9. get-blockchain 查询区块链信息

用于查询指定区块链的详情信息。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID。

用法如下：

```shell
relayer:> get-blockchain --product mychain010 --blockchainId mychain002ly.id
{
	"alias":"",
	"blockchainId":"mychain002ly.id",
	"desc":"",
	"metaKey":"mychain010_mychain002ly.id",
	"pluginServerId":"testps",
	"product":"mychain010",
	"properties":{
		"am_client_contract_address":"{\"evm\":\"AM_EVM_CONTRACT_726a9691-8106-4db8-b0b0-9a54d4a09278\", \"wasm\":\"AM_WASM_CONTRACT_69a2acb3-799b-446b-ba89-44aebed90d77\"}",
		"am_service_status":"DEPLOY_FINISHED",
		"anchor_runtime_status":"RUNNING",
		"extra_properties":{},
		"heterogeneous_bbc_context":{
			"am_contract":{
				"contractAddress":"{\"evm\":\"AM_EVM_CONTRACT_726a9691-8106-4db8-b0b0-9a54d4a09278\", \"wasm\":\"AM_WASM_CONTRACT_69a2acb3-799b-446b-ba89-44aebed90d77\"}",
				"status":"CONTRACT_READY"
			},
			# raw_conf 字段为一段base64编码字符串，解码后为链相关的ssl证书、账户密钥等信息，此处已省略
			"raw_conf":"***",
			"sdp_contract":{
				"contractAddress":"{\"evm\":\"SDP_EVM_CONTRACT_ea0081df-e379-4f30-bb20-1b785c6307e8\", \"wasm\":\"SDP_WASM_CONTRACT_9a106a43-7a0b-4c04-887b-65ac035d41d3\"}",
				"status":"CONTRACT_READY"
			}
		},
		"init_block_height":304702,
		"plugin_server_id":"testps",
		"sdp_msg_contract_address":"{\"evm\":\"SDP_EVM_CONTRACT_ea0081df-e379-4f30-bb20-1b785c6307e8\", \"wasm\":\"SDP_WASM_CONTRACT_9a106a43-7a0b-4c04-887b-65ac035d41d3\"}"
	},
	"running":true
}
```

### 10. get-blockchain-contracts 查询区块链合约信息

用于查询指定区块链已部署的跨链系统合约信息。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID。

用法如下：

```shell
relayer:> get-blockchain-contracts --product mychain010 --blockchainId mychain002ly.id
{"am_contract":"{\"evm\":\"AM_EVM_CONTRACT_726a9691-8106-4db8-b0b0-9a54d4a09278\", \"wasm\":\"AM_WASM_CONTRACT_69a2acb3-799b-446b-ba89-44aebed90d77\"}","state":"DEPLOY_FINISHED","sdp_contract":"{\"evm\":\"SDP_EVM_CONTRACT_ea0081df-e379-4f30-bb20-1b785c6307e8\", \"wasm\":\"SDP_WASM_CONTRACT_9a106a43-7a0b-4c04-887b-65ac035d41d3\"}"}
```

### 11. get-blockchain-heights 查询区块链高度信息

用于查询指定区块链相关高度信息，包括链当前最新高度和链跨链任务执行高度。
当跨链任务执行高度小于链当前最新高度时表明该链存在以提交的跨链任务尚未执行。

参数如下：

- `--product`：（必选）区块链类型，如`mychain010`；
- `--blockchainId`：（必选）区块链标识ID。

用法如下：

```shell
relayer:> get-blockchain-heights --product mychain010 --blockchainId mychain002ly.id
{
    # 该高度为区块链跨链任务执行高度，即该高度以前的跨链任务均已执行，其后的高度尚未执行
	"crosschainTaskBlockHeight":{
		"gmtModified":"2023-12-28 17:00:31",
		"height":484779
	},
	# 该高度为区块链当前最新高度
	"latestBlockHeight":{
		"gmtModified":"2023-12-28 17:00:31",
		"height":484779
	}
}
```

### 12. query-sdpmsg-seq 查询区块链SDP消息序号

用于查询指定发送方和接收方之间有序跨链消息的序号，
每一个「发送区块链-发送合约-接收区块链-接收合约」四元组为有序跨链消息
维护一个从0开始递增的序号。
该命令可以用于查询有序跨链消息的执行情况。


参数如下：

- `--senderDomain`：（必选）发送区块链域名；
- `--sender`：（必选）发送区块链上发送合约地址或标识；
- `--receiverProduct`：（必选）接收区块链类型；
- `--receiverBlockchainId`：（必选）接收区块链标识ID；
- `--receiver`：（必选）接收区块链上接收合约地址或标识。

用法如下：

```shell
？？
```

## 3 中继网络管理

中继CLI工具提供中继网络管理相关指令，用于支持查询中继网络相关信息。

### 1. set-local-endpoints 设置本地中继网络信息

用于设置本地中继器在中继网络中的网络端点信息，包括网络协议、IP地址及端口，可以设置多个，多个网络端点信息用`,`隔开。

命令参数如下：

- `--endpoints`：（必选）本地中继网络信息，如`https://127.0.0.1:8082`。

用法如下：

```shell
relayer:> set-local-endpoints --endpoints https://172.16.0.49:8082,https://172.16.0.50:8082
success
```

### 2. get-local-endpoints 查询本地中继网络信息

用于查询本地中继服务在中继网络中的网络端点信息，包括网络协议、IP地址及端口，可能包含多个。

用法如下：

```shell
relayer:> get-local-endpoints
https://172.16.0.49:8082, https://172.16.0.50:8082
```

### 3. get-local-relayer-id 查询本地中继ID

用于查询本地中继服务在中继网络中的的标识ID。

用法如下：
```shell
relayer:> get-local-relayer-id
5da29c61581c2a54b5442e2efd81e809f49e6267e13ff2a202cf3ac77f8b6f4d
```

### 4. get-local-relayer-cross-chain-certificate 查询本地中继证书

用于查询本地中继服务的域名证书信息，查询成功后将打印PEM格式证书信息。

用法如下：
```shell
relayer:> get-local-relayer-cross-chain-certificate
# 打印信息中部分敏感信息已经替换为`***`
{"credentialSubject":"***","credentialSubjectInstance":{"applicant":{"rawId":"***","type":"BID"},"name":"relay","rawSubjectPublicKey":"***","subject":"***","subjectInfo":"***","subjectPublicKey":{"algorithm":"Ed25519","encoded":"***","format":"X.509","pointEncoding":"***"},"version":"1.0"},"encodedToSign":"***","expirationDate":1733811853,"id":"did:bid:efGeAv4Jr7V2FSyun77m4xTFmTDfG8nh","issuanceDate":1702275853,"issuer":{"rawId":"***","type":"BID"},"proof":{"certHash":"***","hashAlgo":"SM3","rawProof":"***","sigAlgo":"Ed25519"},"type":"RELAYER_CERTIFICATE","version":"1"}
```

### 5. get-local-domain-router 查询本地中继域名路由

用于查询指定域名在本地中继的路由信息，查询成功后会返回该域名所属中继服务的ID及该中继的状态

命令参数如下：

- `--domain`：（必选）待查询区块链域名。

用法如下：

```shell
relayer:> get-local-domain-router --domain chain001.bif
# 可以看到查询域名所属中继服务ID为`b0d02bbb607858ee2868d82058cba117632d59725e77c0243b9fb4321625de01`
# 且该中继服务当前状态为`SYNC`，即连接正常信息同步中
{"nodeId":"b0d02bbb607858ee2868d82058cba117632d59725e77c0243b9fb4321625de01","syncState":"SYNC"}
```

### 6. get-remote-relayer-info 查询远程中继信息

用于查询指定远程中继服务的详情信息，主要包括该中继所包含的链信息及证书相关信息

命令参数如下：

- `--nodeId`：（必选）待查询中继服务标识ID。

用法如下：

```shell
relayer:> get-remote-relayer-info --nodeId b0d02bbb607858ee2868d82058cba117632d59725e77c0243b9fb4321625de01
# 打印信息中部分敏感信息以及替换为`***`
{"domains":["chain001.bif"],"encode":"***","endpoints":["https://172.16.0.49:8182"],"lastTimeHandshake":0,"nodeId":"b0d02bbb607858ee2868d82058cba117632d59725e77c0243b9fb4321625de01","properties":{"lastHandshakeTime":0,"properties":{},"tLSRequired":true},"publicKey":{"algorithm":"Ed25519","encoded":"***","format":"X.509","pointEncoding":"***"},"relayerBlockchainContent":{"relayerBlockchainInfoTrie":{"fib.100niahc":{"amContractClientAddresses":"{\"evm\":\"AM_EVM_CONTRACT_188c6d86-05c8-412c-90a0-288d12617981\", \"wasm\":\"AM_WASM_CONTRACT_205296ca-b4f9-4c00-ab99-1bba1408c120\"}","chainFeatures":{},"domainSpaceChain":[""]}},"trustRootCertTrie":{"":{***}}},"relayerCertId":"did:bid:efWU8DftMNTpXDugKPJDBTbXVWQTSPZT","relayerCredentialSubject":{***},"relayerCrossChainCertificate":{***},"sigAlgo":"Ed25519"}
```

### 7. get-cross-chain-channel 查询跨链通道信息

用于查询两个指定区块链之间的跨链通道信息，主要包括通道两端链所属中继服务节点ID及通道状态。
通道状态包含`CONNECTED`和`DISCONNECTED`两种，状态为`CONNECTED`时可以进行正常跨链。

命令参数如下：

- `--localDomain`：本地区块链域名
- `--remoteDomain`：远程中继的区块链域名

用法如下：

```shell
relayer:> get-cross-chain-channel --localDomain chain002.bif --remoteDomain chain001.bif
# 根据返回信息可以看到该跨链通道状态为`CONNECTED`，即可以进行正常跨链
{"localDomain":"chain002.bif","relayerNodeId":"b0d02bbb607858ee2868d82058cba117632d59725e77c0243b9fb4321625de01","remoteDomain":"chain001.bif","state":"CONNECTED"}
```

## 4 其他服务管理

中继CLI工具提供服务管理相关指令，用于支持插件服务、跨链授权等相关管理功能。

### 1. register-plugin-server 注册（启动）插件服务

用于注册插件服务，注册成功后会自动启动插件服务，已启动的插件服务可以绑定相关类型的区块链并支持链交互等功能。

命令参数如下：

- `--pluginServerId`：插件服务标识ID，可自定义不可重复；
- `--address`：插件服务地址，如`127.0.0.1:9090`；
- `--pluginServerCAPath`：插件服务TLS证书路径，如`/path/to/your/server.crt`。

用法如下：

```shell
relayer:> register-plugin-server --pluginServerId testPS.id --address 127.0.0.1:9090  --pluginServerCAPath /path/to/your/server.crt
success
```

### 2. start-plugin-server 启动插件服务

用于启动插件服务，插件服务首次注册时会自动启动，但如果服务被意外停止或手动停止了，可以使用当前命令启动插件服务。

命令参数如下：

- `--pluginServerId`：插件服务标识ID，可自定义不可重复。

用法如下：

```shell
relayer:> start-plugin-server --pluginServerId testPS.id
success
```

### 3. stop-plugin-server 停止插件服务

用于停止插件服务，插件服务停止后将暂不支持绑定区块链的交互功能。

命令参数如下：

- `--pluginServerId`：插件服务标识ID，可自定义不可重复。

用法如下：

```shell
relayer:> stop-plugin-server --pluginServerId testPS.id
success
```

### 4. add-cross-chain-msg-acl 添加跨链授权

用于添加指定跨链双方的授权信息，跨链双方需要授权通过才能进行跨链交易。

命令参数如下：

- `--ownerDomain`：授权方区块链域名，即接收跨链消息的区块链域名
- `--ownerIdentity`：授权方合约标识，即接收跨链消息区块链上的合约地址或ID
- `--grantDomain`：被授权方区块链域名，即发送跨链消息的区块链域名
- `--grantIdentity`：被授权方合约标识，即发送跨链消息区块链上的合约地址或ID

用法如下：

```shell
relayer:> add-cross-chain-msg-acl --ownerDomain mychain005ly.bif --ownerIdentity cee00ec91971541ff5b767a4fbdcd87962b6668884cfe5fea8fe2689f621a7b6 --grantDomain mychain006ly.bif --grantIdentity b0d964637750a33d10deac8ce0f81beb731ad9dc5be0648a51ba66afde761563
success
```

### 5. get-matched-cross-chain-aclitems 插件跨链授权ID

用于查询指定跨链双方的授权ID，添加授权后中继服务会为指定跨链四元组「发送区块链-发送合约-接收区块链-接收合约」生成唯一的授权ID，当前命令可用于查询该授权ID。

命令参数如下：

- `--ownerDomain`：授权方区块链域名，即接收跨链消息的区块链域名
- `--ownerIdentity`：授权方合约标识，即接收跨链消息区块链上的合约地址或ID
- `--grantDomain`：被授权方区块链域名，即发送跨链消息的区块链域名
- `--grantIdentity`：被授权方合约标识，即发送跨链消息区块链上的合约地址或ID

用法如下：

```shell
relayer:> get-matched-cross-chain-aclitems --ownerDomain mychain005ly.bif --ownerIdentity cee00ec91971541ff5b767a4fbdcd87962b6668884cfe5fea8fe2689f621a7b6 --grantDomain mychain006ly.bif --grantIdentity b0d964637750a33d10deac8ce0f81beb731ad9dc5be0648a51ba66afde761563
your input matched ACL rules : a3b56eea-3a31-43cd-9002-e656f6c3b521
```

### 6. delete-cross-chain-msg-acl 删除跨链授权

用于删除指定授权ID的授权，删除授权后相应的跨链双方无法继续进行跨链交易。

命令参数如下：

- `--bizId`：授权ID，添加授权后中继服务会为跨链信息四元组「发送区块链-发送合约-接收区块链-接收合约」生成唯一的授权ID，`get-matched-cross-chain-aclitems`命令可用于查询该授权ID。

用法如下：

```shell
relayer:> delete-cross-chain-msg-acl --bizId a3b56eea-3a31-43cd-9002-e656f6c3b521
success
```

### 7. get-cross-chain-msg-acl 查询跨链授权信息

用于查询指定授权ID的授权信息，授权信息主要包括授权方（接收方）区块链域名及合约标识、被授权方（发送方）区块链域名及合约标识。

命令参数如下：

- `--bizId`：授权ID，添加授权后中继服务会为跨链信息四元组「发送区块链-发送合约-接收区块链-接收合约」生成唯一的授权ID，`get-matched-cross-chain-aclitems`命令可用于查询该授权ID。

用法如下：

```shell
# 查询存在的跨链授权
relayer:> get-cross-chain-msg-acl --bizId 57168a55-aeb1-41f9-aedc-0534db81ab41
{"bizId":"57168a55-aeb1-41f9-aedc-0534db81ab41","grantDomain":"mychain006ly.bif","grantIdentity":"b0d964637750a33d10deac8ce0f81beb731ad9dc5be0648a51ba66afde761563","grantIdentityHex":"b0d964637750a33d10deac8ce0f81beb731ad9dc5be0648a51ba66afde761563","isDeleted":0,"ownerDomain":"mychain005ly.bif","ownerIdentity":"cee00ec91971541ff5b767a4fbdcd87962b6668884cfe5fea8fe2689f621a7b6","ownerIdentityHex":"cee00ec91971541ff5b767a4fbdcd87962b6668884cfe5fea8fe2689f621a7b6"}

# 查询不存在的跨链授权，已删除的跨链授权将不存在
relayer:> get-cross-chain-msg-acl --bizId a3b56eea-3a31-43cd-9002-e656f6c3b521
not found
```

## 5 其他工具命令

为方便用户使用，中继CLI工具提供部分工具命令，包括账户文件生成、域名申请文件生成等辅助功能。

### 5.1 generate-relayer-account 生成跨链身份密钥

用于生成跨链身份的密钥文件，跨链平台中有中继身份、PTC身份和区块链身份等，
每个身份都需要想BCDNS服务申请域名证书（申请命令见1.7`apply-domain-name-cert`），
申请证书需要提供身份密钥信息。
当前命令可以在指定目录生成一对指定密钥算法的公私钥文件，并打印出生成路径信息。

命令参数如下：

- `--keyAlgo`：（可选）密钥生成算法，目前仅支持`Ed25519`，默认为`Ed25519`
- `--outDir`：（可选）密钥生成路径，需要是已存在路径，默认为当前路径

用法如下：

```shell
relayer:> generate-relayer-account --outDir /root/liyuan/20231228
private key path: /root/liyuan/20231228/private_key.pem
public key path: /root/liyuan/20231228/public_key.pem

# 退出CLI界面查看指定路径
$ tree
.
├── private_key.pem
└── public_key.pem

0 directories, 2 files
```

### 5.2 generate-bid-document 生成BID文件

用于生成申请BID类型域名的BID文件，BID文件将包含账户公钥信息。

命令参数如下：

- `--publicKeyPath`：（必选）账户公钥路径，可以为`generate-relayer-account`命令生成的公钥路径
- `--outDir`：（可选）BID生成路径，需要是已存在路径，默认为当前路径

用法如下：

```shell
relayer:> generate-bid-document --publicKeyPath /root/liyuan/20231228/public_key.pem --outDir /root/liyuan/20231228
file is : /root/liyuan/20231228/bid_document.json

# 退出CLI界面查看BID文件
$ cat bid_document.json
{"publicKey":[{"type":"ED25519","publicKeyHex":"b0656681726b60deaf030d11bb4282fa80a30323d661f73e56ec31cb1cb183508781e7"}]}
```

### 5.3 generate-bif-bcdns-conf 生成BCDNS客户端文件

用于生成 BIF BCDNS 客户端配置文件，文件名默认为

命令参数如下：

- `--relayerPrivateKeyFile`：（必选）中继私钥路径；
- `--relayerCrossChainCertFile`：（必选）中继跨链证书路径；
- `--relayerSigAlgo`：（必选）中继私钥的密钥算法，默认为`Ed25519`；
- `--authPrivateKeyFile`：（可选）BIF BCDNS 授权给中继或PTC的证书私钥路径，默认使用中继私钥；
- `--authPublicKeyFile`：（可选）BIF BCDNS 授权给中继或PTC的证书公钥路径，默认使用给中继公钥；
- `--authSigAlgo`：（可选）BIF BCDNS 授权证书的密钥签名算法，默认为`Ed25519`；
- `--certServerUrl`：（必选）BIF BCDNS 证书服务的url，如`http://localhost:8112`；
- `--bifChainRpcUrl`：（必选）BIF 区块链节点的RPC url，如`http://test.bifcore.bitfactory.cn`；
- `--bifChainRpcPort`：（可选）BIF 区块链节点的RPC端口，默认为`-1`即不需要端口；
- `--bifDomainGovernContract`：（必选）BIF 链上的区块链域名管理合约地址；
- `--bifRelayerGovernContract`：（必选）BIF 链上的中继服务域名管理合约地址；
- `--bifPtcGovernContract`：（必选）BIF 链上的PTC服务域名管理合约地址。
- `--outDir`：（可选）客户端配置文件生成路径，需要是已存在路径，默认为当前路径，文件名为`bif_bcdns_conf.json`。

用法如下：

```shell

```