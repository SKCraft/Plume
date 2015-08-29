package com.skcraft.plume.common.util.module;

import com.google.inject.ScopeAnnotation;
import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@IndexAnnotated
@ScopeAnnotation
public @interface Module {

    String name();

    String desc() default "";

    boolean enabled() default true;

    boolean hidden() default false;

}
