package com.helmx.tutorial.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String value() default "";

    /**
     * SpEL expression to extract the resource name (container/image/volume/network name)
     * from the method arguments. For example: "#criteria.name", "#criteria.containerId".
     * Supports standard SpEL syntax with method parameter names as variables (e.g. #paramName).
     */
    String resourceName() default "";
}
