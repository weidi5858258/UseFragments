package com.weidi.usefragments.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectExtra {
    String bundle() default "";

    String key() default "";
}
/**
 * ]
 *
 * @InjectExtra(bundle = "bundle", key = "test1")
 * private String test1;
 * @InjectExtra(bundle = "bundle")
 * private Bundle bundle;
 * @InjectExtra(key = "test9")
 * private Object test9;
 * @param activity
 */
