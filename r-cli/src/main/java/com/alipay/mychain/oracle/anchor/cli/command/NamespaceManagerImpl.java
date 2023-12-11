/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.util.ArrayList;
import java.util.List;

import com.alipay.mychain.oracle.anchor.cli.groovyshell.command.BCDNSManagerCmdNamespace;
import com.alipay.mychain.oracle.anchor.cli.groovyshell.command.BlockchainManagerCmdNamespace;


/**
 *  @author honglin.qhl
 *  @version $Id: NamespaceManagerImpl.java, v 0.1 2017-06-15 下午5:09 honglin.qhl Exp $$
 */
public class NamespaceManagerImpl implements NamespaceManager {

    private List<CommandNamespace> commandNamespaces = new ArrayList<>();

    public NamespaceManagerImpl() {
        super();
        init();
    }

    /**
     * init commandNamespaces
     */
    private void init() {
        this.addNamespace(new BCDNSManagerCmdNamespace());
        this.addNamespace(new BlockchainManagerCmdNamespace());
    }

    @Override
    public void addNamespace(CommandNamespace commandNamespace) {

        this.commandNamespaces.add(commandNamespace);
    }

    @Override
    public List<CommandNamespace> getCommandNamespaces() {
        return commandNamespaces;
    }

    @Override
    public String dump() {

        StringBuilder builder = new StringBuilder();

        commandNamespaces.forEach(commandNamespace -> {

            builder.append("\n").append(commandNamespace.name());

            CommandNamespaceImpl abstraceNamespace = (CommandNamespaceImpl) commandNamespace;

            abstraceNamespace.getCommands().forEach((cmdName, cmd) -> {

                builder.append("\n\t.").append(cmdName);
                if (!cmd.getArgs().isEmpty()) {
                    builder.append("(");
                    cmd.getArgs().forEach(arg -> {
                        builder.append(arg.getName()).append(",");
                    });
                    builder.deleteCharAt(builder.length() - 1);
                    builder.append(")");

                } else {
                    builder.append("()");
                }
            });
        });

        return builder.append("\n\n").toString();
    }
}
