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

package com.alipay.antchain.bridge.relayer.cli.shell;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.alipay.antchain.bridge.relayer.cli.command.NamespaceManager;
import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import com.alipay.antchain.bridge.relayer.cli.groovyshell.Launcher;
import com.alipay.antchain.bridge.relayer.cli.util.JsonUtil;
import lombok.Getter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class Shell {

    private static final String PROMPT = "\n\033[0;37mrelayer> \033[0m";

    private final NamespaceManager namespaceManager;

    private GlLineReader reader;

    private final Map<String, ReservedWord> reservedWord = new HashMap<>();

    private final AtomicBoolean loopRunning = new AtomicBoolean(false);

    private final ReentrantLock shellLock = new ReentrantLock();

    private final ShellProvider shellProvider;

    public final static Runtime Runtime = new Runtime();

    public Shell(ShellProvider shellProvider, PromptCompleter completer, GrpcClient grpcClient,
                 NamespaceManager namespaceManager) {

        // Non-extensible parameter initialization
        init();

        // Extended initialization
        this.shellProvider = shellProvider;
        this.namespaceManager = namespaceManager;

        this.reservedWord.keySet().forEach(completer::addReservedWord);

        reader.setCompleter(completer);

        // Run time Settings
        Runtime.setGrpcClient(grpcClient);
    }

    void init() {
        // init term
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("can't open system stream");
        }

        // set printer
        Runtime.setPrinter(terminal.writer());

        // init line reader
        reader = new GlLineReader(terminal, "mychain-gl", new HashMap<>());

        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("./clihistory.tmp"));
        reader.setHistory(new DefaultHistory(reader));

        reader.unsetOpt(Option.MENU_COMPLETE);
        reader.setOpt(Option.AUTO_LIST);
        reader.unsetOpt(Option.AUTO_MENU);


        // init shell commands
        initReservedWord();
    }

    public void start() {

        try {
            if (shellLock.tryLock()) {

                if (loopRunning.get()) {
                    return;
                }

                loopRunning.set(true);

                welcome();

                new Thread(() -> {
                    // start loop
                    while (loopRunning.get()) {
                        String cmd = reader.readLine(PROMPT);

                        if (null == cmd || cmd.isEmpty()) {
                            continue;
                        }

                        try {
                            if (reservedWord.containsKey(cmd.trim())) {

                                reservedWord.get(cmd.trim()).execute();
                                continue;
                            }

                            String result = this.shellProvider.execute(cmd);


                            if (null != result) {
                                if (result.startsWith("{") || result.startsWith("[")) {
                                    Runtime.getPrinter().println(JsonUtil.format(result));
                                } else {
                                    Runtime.getPrinter().println(result);
                                }

                            }
                        } catch (Exception e) {
                            Runtime.getPrinter().println("shell evaluate fail:" + e.getMessage());
                        }
                    }
                }, "shell_thread").start();

            }
        } finally {
            shellLock.unlock();
        }
    }

    public String execute(String cmd) {
        return this.shellProvider.execute(cmd);
    }

    public void stop() {

        loopRunning.set(false);
        try {
            if (null != Runtime.getGrpcClient()) {
                Runtime.getGrpcClient().shutdown();
            }
        } catch (InterruptedException e) {
            // not process
        }
    }

    protected void initReservedWord() {
        this.reservedWord.put("exit", this::exit);
        this.reservedWord.put("help", this::help);
    }

    protected void exit() {
        stop();
    }

    protected void help() {
        Runtime.getPrinter().print(namespaceManager.dump());
    }

    protected void welcome() {
        Runtime.printer.println(
                "    ___    ______ ____     ____   ______ __     ___ __  __ ______ ____\n" +
                "   /   |  / ____// __ )   / __ \\ / ____// /    /   |\\ \\/ // ____// __ \\\n" +
                "  / /| | / /    / __  |  / /_/ // __/  / /    / /| | \\  // __/  / /_/ /\n" +
                " / ___ |/ /___ / /_/ /  / _, _// /___ / /___ / ___ | / // /___ / _, _/\n" +
                "/_/  |_|\\____//_____/  /_/ |_|/_____//_____//_/  |_|/_//_____//_/ |_|\n\n" +
                "                               CLI " + Launcher.getVersion()
        );
        Runtime.printer.println("\n>>> type help to see all commands...\n");
    }

    @Getter
    public static class Runtime {

        private PrintWriter printer;

        private GrpcClient grpcClient;

        void setPrinter(PrintWriter printer) {

            this.printer = printer;
        }

        void setGrpcClient(GrpcClient grpcClient) {
            this.grpcClient = grpcClient;
        }

    }
}
