/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.groovyshell;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.stream.Stream;

import com.alipay.mychain.oracle.anchor.cli.command.ArgsConstraint;
import com.alipay.mychain.oracle.anchor.cli.command.Command;
import com.alipay.mychain.oracle.anchor.cli.command.CommandNamespaceImpl;
import com.alipay.mychain.oracle.anchor.cli.shell.Shell;
import com.alipay.mychain.oracle.servicemanager.grpcserver.grpc.admin.AdminRequest;
import com.alipay.mychain.oracle.servicemanager.grpcserver.grpc.admin.AdminResponse;
import com.google.common.collect.Lists;

/**
 *  @author honglin.qhl
 *  @version $Id: GroovyScriptCommandNamespace.java, v 0.1 2017-06-17 下午4:44 honglin.qhl Exp $$
 */
public abstract class GroovyScriptCommandNamespace extends CommandNamespaceImpl {

    private static String COMMAND_NAMESPACE_NAME = "name";

    /**
     * 命名空间名称由子类实现
     *
     * @return
     */
    @Override
    public abstract String name();

    public GroovyScriptCommandNamespace() {
        super();
        loadCommand();
    }

    /**
     * 初始化:加载command,默认将子类所有方法解析为命令
     */
    public void loadCommand() {

        Method[] methods = this.getClass().getDeclaredMethods();

        Stream.of(methods).forEach(method -> {

            if (COMMAND_NAMESPACE_NAME.equals(method.getName())) {
                return;
            }

            Command cmd = new Command(method.getName());

            Parameter[] params = method.getParameters();

            for (Parameter param : params) {

                String argName = param.getName();
                List<String> constraints = Lists.newArrayList();

                ArgsConstraint argsConstraint = param.getAnnotation(ArgsConstraint.class);

                if (null != argsConstraint) {
                    if (null != argsConstraint.name() && !"".equals(argsConstraint.name().trim())) {
                        argName = argsConstraint.name().trim();
                    }
                    if (null != argsConstraint.constraints()) {
                        Stream.of(argsConstraint.constraints()).filter(
                            constraint -> null != constraint && !"".equals(constraint.trim())).forEach(
                            constraint -> constraints.add(constraint));
                    }
                }

                cmd.addArgs(argName, param.getType().getSimpleName(), constraints);
            }
            addCommand(cmd);
        });
    }

    protected String queryAPI(String command, Object... args) {

        if (args != null) {
            String[] strArgs = new String[args.length];
            for (int i = 0; i < args.length; ++i) {
                strArgs[i] = args[i].toString();
            }

            return queryAPI(command, strArgs);
        } else {

            return queryAPI(command);
        }
    }

    /**
     * 查询api,供子类命令执行使用
     *
     * @param command
     * @param args
     * @return
     */
    protected String queryAPI(String command, String... args) {

        AdminRequest.Builder reqBuilder = AdminRequest.newBuilder();

        reqBuilder.setCommandNamespace(name());
        reqBuilder.setCommand(command);
        if (null != args) {
            reqBuilder.addAllArgs(Lists.newArrayList(args));
        }

        try {
            AdminResponse response = Shell.Runtime.getGrpcClient().adminRequest(reqBuilder.build());

            if (response.getSuccess()) {
                return response.getResult();
            } else {
                print(response.getErrorMsg());
                return null;
            }
        } catch (Exception e) {
            print(e.getMessage());
            return null;
        }
    }

    protected void print(String result) {

        Shell.Runtime.getPrinter().println(result);
    }
}
