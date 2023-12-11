/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.Map;

/**
 * 命令命名空间
 *
 * @author honglin.qhl
 * @version $Id: CmdNamespace.java, v 0.1 2017-06-14 下午9:32 honglin.qhl Exp $$
 */
public interface CommandNamespace {

    /**
     * 命名空间名称
     *
     * @return
     */
    public String name();

    /**
     * 获取命令命名空间下所有命令
     *
     * @return
     */
    public Map<String, Command> getCommands();
}
