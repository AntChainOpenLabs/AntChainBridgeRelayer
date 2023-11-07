package com.alipay.antchain.bridge.relayer.core.manager.network;

import java.util.List;

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.relayer.commons.constant.RelayerNodeSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerHealthInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNetwork;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;

/**
 * 该Manager提供个管理RelayerNetwork的系列管理接口
 */
public interface IRelayerNetworkManager {

    //**********************************************
    // relayer节点管理
    //**********************************************

    /**
     * 获取Relayer自身基本信息
     *
     * @return
     */
    RelayerNodeInfo getRelayerNodeInfo();

    /**
     * 获取Relayer自身基本信息，并且带上relayerBlockchainInfos。
     *
     * @return
     */
    RelayerNodeInfo getRelayerNodeInfoWithContent();

    /**
     * 获取对应domain的relayerBlockchainInfos。
     *
     * @param domain
     * @return
     */
    RelayerBlockchainInfo getRelayerBlockchainInfo(String domain);

    /**
     * 添加Relayer节点，仅仅只是新增relayer节点信息到数据库，需要另外调用syncRelayerNode去同步节点信息
     *
     * @param nodeInfo
     * @return
     */
    void addRelayerNode(RelayerNodeInfo nodeInfo);

    /**
     * 将除了域名列表和RelayerBlockchainInfo之外的信息存储起来
     *
     * @param nodeInfo
     * @return
     */
    void addRelayerNodeWithoutDomainInfo(RelayerNodeInfo nodeInfo);

    /**
     * 向relayer node中添加属性
     *
     * @param nodeId
     * @param key
     * @param value
     * @return
     */
    void addRelayerNodeProperty(String nodeId, String key, String value);

    /**
     * 获取Relayer节点
     *
     * @param nodeId
     * @return
     */
    RelayerNodeInfo getRelayerNode(String nodeId);

    /**
     * 同步网络里Relayer节点信息，该同步动作会向远程relayer节点请求信息，包括：
     * <li>预言机Evidence
     * <li>支持的domain
     * <li>支持的domain对应的预言机签名udns（含预言机c_key、预言机签名）
     *
     * 得到以上信息后，会执行对应的校验
     * <li>使用本地配置的预言机信任根校验预言机Evidence（如果本地没有配置信任根，则会不会校验信任根，并且将远程信任根设置为信任根）
     * <li>校验relayer合法持有domain（避免路由欺诈）
     * <li>校验预言机签名udns的合法性
     *
     * 以上步骤均完成后，节点状态就变更为sync状态，表示已完成元信息同步
     *
     * @param networkId 网络id
     * @param nodeId    节点id
     * @return
     */
    void syncRelayerNode(String networkId, String nodeId);

    //**********************************************
    // relayer 网络管理
    //**********************************************

    /**
     * 查找domain name所在的网络
     *
     * @param domainName
     * @return
     */
    RelayerNetwork findNetworkByDomainName(String domainName);

    /**
     * 新建Relayer网络
     *
     * @param network
     * @return
     */
    boolean addRelayerNetwork(RelayerNetwork network);

    /**
     * 往Relayer网络新增路由信息
     *
     * @param domain
     * @param nodeId
     * @return
     */
    boolean addRelayerNetworkItem(String networkId, String domain, String nodeId);

    /**
     * 往Relayer网络新增路由信息
     *
     * @param networkId
     * @param domain
     * @param nodeId
     * @param syncState
     * @return
     */
    boolean addRelayerNetworkItem(String networkId, String domain, String nodeId, RelayerNodeSyncStateEnum syncState);

    /**
     * 删除对应的item
     *
     * @param domain
     * @param nodeId
     * @return
     */
    boolean deleteRelayerNetworkItem(String domain, String nodeId);

    /**
     * 获取指定id的网络
     *
     * @param networkId
     * @return
     */
    RelayerNetwork getRelayerNetwork(String networkId);

    /**
     * 获取所有Relayer网络
     *
     * @return
     */
    List<RelayerNetwork> getRelayerNetworks();

    /**
     * 获取domain对应的relayer的Node Info
     *
     * @param domain
     * @return
     */
    RelayerNodeInfo getRelayerNodeInfoForDomain(String domain);

    /**
     * 从DiscoveryServer获取对应域名的relayer node info。
     * @param domain
     * @return
     */
    RelayerNodeInfo getRemoteRelayerNodeInfo(String domain);

    /**
     * 注册域名到DiscoveryServer
     *
     * @param nodeInfo
     */
    void registerDomainToDiscoveryServer(RelayerNodeInfo nodeInfo, String networkId) throws Exception;

    /**
     * 更新域名到DiscoveryServer
     *
     * @param nodeInfo
     */
    void updateDomainToDiscoveryServer(RelayerNodeInfo nodeInfo) throws Exception;

    /**
     * 删除域名到DiscoveryServer
     *
     * @param nodeInfo
     */
    void deleteDomainToDiscoveryServer(RelayerNodeInfo nodeInfo) throws Exception;

    /**
     * 在amRequest和udagRequest的时候，从发现中心获取域名对应的relayer信息，
     * 尝试握手获取并验证对应的信息，存储域名到数据库。
     *
     * @param domainName
     * @return
     */
    boolean tryHandshake(String domainName, RelayerNodeInfo remoteNodeInfo);

    /**
     * 更新relayerNodeInfo
     *
     * @param nodeInfo
     * @return
     */
    boolean updateRelayerNode(RelayerNodeInfo nodeInfo);

    /**
     * Obtain health information about the relayer node, including the node ip address, port number, and whether the node is alive
     *
     * @return
     */
    List<RelayerHealthInfo> healthCheckRelayers();

    /**
     * @param relayerRequest
     */
    void signRelayerRequest(RelayerRequest relayerRequest);

    /**
     * @param relayerResponse
     */
    void signRelayerResponse(RelayerResponse relayerResponse);

    AbstractCrossChainCertificate getLocalRelayerCertificate();

    boolean validateRelayerRequest(RelayerRequest relayerRequest);

    boolean validateRelayerResponse(RelayerResponse relayerResponse);

    String getLocalNodeId();

    String getLocalNodeSigAlgo();
}
