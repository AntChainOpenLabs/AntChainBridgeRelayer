/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

/**
 *  @author honglin.qhl
 *  @version $Id: CommandHandler.java, v 0.1 2017-06-17 下午5:58 honglin.qhl Exp $$
 */
@FunctionalInterface
public interface CommandHandler {

    /**
     * 执行命令
     *
     * @param namespace 命令空间
     * @param command   命令
     * @param params    执行参数
     * @return
     */
    Object execute(String namespace, String command, String... params);
}
