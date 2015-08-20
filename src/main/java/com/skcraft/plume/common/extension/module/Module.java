package com.skcraft.plume.common.extension.module;

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

    boolean enabled() default true;

}
