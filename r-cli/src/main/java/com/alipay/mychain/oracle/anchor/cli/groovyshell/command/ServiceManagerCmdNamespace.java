/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.groovyshell.command;

import com.alipay.mychain.oracle.anchor.cli.command.ArgsConstraint;
import com.alipay.mychain.oracle.anchor.cli.groovyshell.GroovyScriptCommandNamespace;

/**
 *  @author honglin.qhl
 *  @version $Id: OracleManagerCmdNamespace.java, v 0.1 2017-06-17 下午1:40 honglin.qhl Exp $$
 */
public class ServiceManagerCmdNamespace extends GroovyScriptCommandNamespace {

    /**
     * the name prompt to user
     *
     * @return
     */
    @Override
    public String name() {
        return "serviceManager";
    }


    Object addCrossChainMsgACL(@ArgsConstraint(name = "grantDomain") String grantDomain,
                               @ArgsConstraint(name = "grantIdentity") String grantIdentity,
                               @ArgsConstraint(name = "ownerDomain") String ownerDomain,
                               @ArgsConstraint(name = "ownerIdentity") String ownerIdentity) {

        return queryAPI("addCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    Object getCrossChainMsgACL(@ArgsConstraint(name = "bizId") String bizId) {

        return queryAPI("getCrossChainMsgACL", bizId);
    }

    Object getMatchedCrossChainACLItems(@ArgsConstraint(name = "grantDomain") String grantDomain,
                                        @ArgsConstraint(name = "grantIdentity") String grantIdentity,
                                        @ArgsConstraint(name = "ownerDomain") String ownerDomain,
                                        @ArgsConstraint(name = "ownerIdentity") String ownerIdentity) {

        return queryAPI("getCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    Object deleteCrossChainMsgACL(@ArgsConstraint(name = "bizId") String bizId) {

        return queryAPI("deleteCrossChainMsgACL", bizId);
    }


       Object registerPluginServer(
            @ArgsConstraint(name = "pluginServerId") String pluginServerId,
            @ArgsConstraint(name = "address") String address,
            @ArgsConstraint(name = "pluginServerCAPath") String pluginServerCAPath
    ) {
        return queryAPI("registerPluginServer", pluginServerId, address, pluginServerCAPath);
    }

    Object stopPluginServer(
            @ArgsConstraint(name = "pluginServerId") String pluginServerId
    ) {
        return queryAPI("stopPluginServer", pluginServerId);
    }

    Object startPluginServer(
            @ArgsConstraint(name = "pluginServerId") String pluginServerId
    ) {
        return queryAPI("startPluginServer", pluginServerId);
    }
}
