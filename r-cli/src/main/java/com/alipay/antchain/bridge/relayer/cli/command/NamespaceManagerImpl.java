/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.cli.command;

import java.util.ArrayList;
import java.util.List;

import com.alipay.antchain.bridge.relayer.cli.groovyshell.command.BCDNSManagerCmdNamespace;
import com.alipay.antchain.bridge.relayer.cli.groovyshell.command.BlockchainManagerCmdNamespace;
import com.alipay.antchain.bridge.relayer.cli.groovyshell.command.ServiceManagerCmdNamespace;


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
        this.addNamespace(new ServiceManagerCmdNamespace());
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
