
package com.alipay.mychain.oracle.anchor.cli.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArgsConstraint {

    String name() default "";

    String[] constraints() default "";
}
