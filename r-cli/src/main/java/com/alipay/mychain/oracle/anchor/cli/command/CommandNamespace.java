/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.Map;

public interface CommandNamespace {

    /**
     * Namespace name.
     *
     * @return
     */
    public String name();

    /**
     * Get all commands in the command namespace.
     *
     * @return
     */
    public Map<String, Command> getCommands();
}
