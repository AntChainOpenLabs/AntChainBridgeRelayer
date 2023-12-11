/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.groovyshell;

import java.beans.Introspector;

import com.alipay.mychain.oracle.anchor.cli.command.NamespaceManager;
import com.alipay.mychain.oracle.anchor.cli.shell.ShellProvider;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 *  @author honglin.qhl
 *  @version $Id: GroovyShellProvider.java, v 0.1 2017-06-17 下午5:44 honglin.qhl Exp $$
 */
public class GroovyShellProvider extends GroovyShell implements ShellProvider {

    private NamespaceManager namespaceManager;


    public GroovyShellProvider(NamespaceManager namespaceManager) {
        this.namespaceManager = namespaceManager;

        // init GroovyShell
        this.namespaceManager.getCommandNamespaces().forEach(namespace -> {

            // only load GroovyScriptCommandNamespace
            if (namespace instanceof GroovyScriptCommandNamespace) {
                this.setVariable(namespace.name(), namespace);
            }
        });
    }

    private int cleanCount = 0;

    private static int CLEAN_PERIOD = 20;

    @Override
    public String execute(String cmd) {
        Script shell = this.parse(cmd);
        Object scriptObject = InvokerHelper.createScript(shell.getClass(), this.getContext()).run();

        // 周期清除缓存，防止OOM
        if((++cleanCount) % CLEAN_PERIOD == 0) {
            getClassLoader().clearCache();
            ClassInfo.clearModifiedExpandos();
            Introspector.flushCaches();
        }

        // execute by groovy script
        return scriptObject.toString();
    }

    @Override
    public void shutdowm() {
        // nothing
    }
}
