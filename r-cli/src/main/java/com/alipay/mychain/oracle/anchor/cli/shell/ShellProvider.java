/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.shell;

/**
 *  @author honglin.qhl
 *  @version $Id: ShellProvider.java, v 0.1 2017-06-17 下午5:41 honglin.qhl Exp $$
 */
public interface ShellProvider {

    /**
     * 执行命令
     *
     * @param cmd
     */
    String execute(String cmd);

    /**
     * shutdown
     */
    void shutdowm();
}
