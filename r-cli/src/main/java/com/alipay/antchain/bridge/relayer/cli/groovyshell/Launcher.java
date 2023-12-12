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

package com.alipay.antchain.bridge.relayer.cli.groovyshell;

import java.io.*;

import com.alipay.antchain.bridge.relayer.cli.command.NamespaceManager;
import com.alipay.antchain.bridge.relayer.cli.command.NamespaceManagerImpl;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import com.alipay.antchain.bridge.relayer.cli.shell.PromptCompleter;
import com.alipay.antchain.bridge.relayer.cli.shell.Shell;
import com.alipay.antchain.bridge.relayer.cli.shell.ShellProvider;
import com.alipay.antchain.bridge.relayer.cli.util.CliConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;


/**
 *  @author honglin.qhl
 *  @version $Id: Launcher.java, v 0.1 2017-06-15 下午8:17 honglin.qhl Exp $$
 */
@Slf4j
public class  Launcher {

    private static final String OP_HELP = "h";
    private static final String OP_VERSION = "v";
    private static final String OP_PORT = "p";
    private static final String OP_CMD = "c";
    private static final String OP_FILE = "f";
    private static final String OP_HOST = "H";

    private static Options options;

    static {
        options = new Options();
        options.addOption(OP_HELP, "help", false, "print help info");
        options.addOption(OP_VERSION, "version", false, "print version info");
        options.addOption(OP_PORT, "port", true, "admin server port");
        options.addOption(OP_CMD, "command", true, "execute the command");
        options.addOption(OP_FILE, "file", true, "execute the file with multi line command");
        options.addOption(OP_HOST, "host", true, "set the host");
    }

    public static void main(String[] args) throws ParseException {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(OP_HELP)) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("", options);
            return;
        }

        if (cmd.hasOption(OP_VERSION)) {
            log.info("cliVersion : {}", CliConstant.CLI_VERSION);
            return;
        }

        //default port : 9393
        int port = 9393;
        if (cmd.hasOption(OP_PORT)) {
            port = Integer.valueOf(cmd.getOptionValue(OP_PORT));
        }

        // new namespace
        NamespaceManager namespaceManager = new NamespaceManagerImpl();

        // new shellProvider
        ShellProvider shellProvider = new GroovyShellProvider(namespaceManager);

        // new promptCompleter
        PromptCompleter promptCompleter = new PromptCompleter();
        promptCompleter.addNamespace(namespaceManager);

        // new grpcClient
        String host = "localhost";
        if (cmd.hasOption(OP_HOST)) {
            host = cmd.getOptionValue(OP_HOST);
        }
        GrpcClient grpcClient = new GrpcClient(host, port);

        if (!grpcClient.checkServerStatus()) {
            log.info("start fail, can't connect to local server.port: {}", port);
            return;
        }

        // new shell
        Shell shell = new Shell(shellProvider, promptCompleter, grpcClient, namespaceManager);

        if (cmd.hasOption(OP_CMD)) {
            String command = cmd.getOptionValue(OP_CMD);

            try {
                String result = shell.execute(command);
                log.info(result);
            } catch (Exception e) {
                log.info("illegal command[{}], execute fail", command);
            }
            return;
        }

        if (cmd.hasOption(OP_FILE)) {
            String filePath = cmd.getOptionValue(OP_FILE);

            String command = "";
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));

                command = reader.readLine();
                StringBuilder resultBuilder = new StringBuilder();
                while (null != command) {
                    try {
                        String result = shell.execute(command);
                        resultBuilder.append(result).append("\n");
                    } catch (Exception e) {
                        resultBuilder.append("error\n");
                    }
                    command = reader.readLine();
                }

                log.info(resultBuilder.toString());

            } catch (FileNotFoundException e) {
                log.info("error: file not found");
            } catch (IOException e) {
                log.info("error: io exception");
            } catch (Exception e) {
                log.info("illegal command[{}], execute fail", command);
            }

            return;
        }

        shell.start();

        Runtime.getRuntime().addShutdownHook(new Thread(shell::stop));
    }
}
