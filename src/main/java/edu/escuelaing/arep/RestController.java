package edu.escuelaing.arep;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Without RUNTIME, Java removes this annotation at compile time.
@Retention(RetentionPolicy.RUNTIME)

@Target(ElementType.TYPE)


public @interface RestController {
    
}
