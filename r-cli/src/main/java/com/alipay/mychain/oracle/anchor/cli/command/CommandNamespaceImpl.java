/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.HashMap;
import java.util.Map;

/**
 *  @author honglin.qhl
 *  @version $Id: CommandNamespaceImpl.java, v 0.1 2017-06-14 下午9:42 honglin.qhl Exp $$
 */
public class CommandNamespaceImpl implements CommandNamespace {
    /**
     * 命令集合
     */
    private Map<String, Command> commands = new HashMap<>();

    /**
     * 命名空间名称
     *
     * @return
     */
    @Override
    public String name() {
        return null;
    }

    /**
     * 往命名空间添加一个命令
     *
     * @param cmd
     */
    public void addCommand(Command cmd) {
        commands.put(cmd.getName(), cmd);
    }

    /**
     * 获取命名空间下所有命令
     *
     * @return
     */
    public Map<String, Command> getCommands() {
        return commands;
    }
}
