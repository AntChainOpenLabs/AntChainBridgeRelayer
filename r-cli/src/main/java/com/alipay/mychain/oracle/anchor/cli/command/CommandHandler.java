package com.alipay.mychain.oracle.anchor.cli.command;

@FunctionalInterface
public interface CommandHandler {

    /**
     * execute command
     *
     * @param namespace
     * @param command
     * @param params
     * @return
     */
    Object execute(String namespace, String command, String... params);
}
