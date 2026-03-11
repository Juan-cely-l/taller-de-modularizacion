package edu.escuelaing.arep;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)

@Target(ElementType.METHOD) // Only on methods



public @interface GetMapping {
    String value();
    
}
