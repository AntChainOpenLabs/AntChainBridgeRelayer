/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.List;

public interface NamespaceManager {

    /**
     * add namespace
     *
     * @param commandNamespace
     */
    void addNamespace(CommandNamespace commandNamespace);

    /**
     * get all namespace
     *
     * @return
     */
    List<CommandNamespace> getCommandNamespaces();

    /**
     * namespace snapshot
     *
     * @return
     */
    String dump();
}
