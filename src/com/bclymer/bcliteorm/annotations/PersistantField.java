package com.bclymer.bcliteorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PersistantField {
	String columnName() default "";
	boolean id() default false;
	boolean generatedId() default false;
}
