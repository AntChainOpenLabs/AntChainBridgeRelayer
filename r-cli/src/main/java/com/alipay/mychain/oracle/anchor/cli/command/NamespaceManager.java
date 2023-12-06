/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.List;

/**
 *  @author honglin.qhl
 *  @version $Id: NamespaceManager.java, v 0.1 2017-06-17 下午8:22 honglin.qhl Exp $$
 */
public interface NamespaceManager {

    /**
     * 添加namespace
     *
     * @param commandNamespace
     */
    void addNamespace(CommandNamespace commandNamespace);

    /**
     * 获取所有namespace
     *
     * @return
     */
    List<CommandNamespace> getCommandNamespaces();

    /**
     * namespace快照
     *
     * @return
     */
    String dump();
}
