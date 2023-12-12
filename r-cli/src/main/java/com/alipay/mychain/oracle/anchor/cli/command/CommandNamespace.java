
package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.Map;

public interface CommandNamespace {

    /**
     * namespace name
     *
     * @return
     */
    public String name();

    /**
     * get all commands in the command namespace
     *
     * @return
     */
    public Map<String, Command> getCommands();
}
