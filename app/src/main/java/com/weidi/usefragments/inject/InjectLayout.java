package com.weidi.usefragments.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @Target注解作用对象（作用域）
// ElementType.TYPE:作用在类上面
@Target(ElementType.TYPE)
// @Retention注解的生命周期
// RetentionPolicy.RUNTIME
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectLayout {
    int value();
}
