/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  @author honglin.qhl
 *  @version $Id: ArgsConstraint.java, v 0.1 2017-06-14 下午9:46 honglin.qhl Exp $$
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgsConstraint {

    String name() default "";

    String[] constraints() default "";
}
