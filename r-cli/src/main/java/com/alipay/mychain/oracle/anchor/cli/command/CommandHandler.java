/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

@FunctionalInterface
public interface CommandHandler {

    /**
     * Execute command
     *
     * @param namespace
     * @param command
     * @param params
     * @return
     */
    Object execute(String namespace, String command, String... params);
}
