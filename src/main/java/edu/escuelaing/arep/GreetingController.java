package edu.escuelaing.arep;

import java.util.concurrent.atomic.AtomicLong;

@RestController


public class GreetingController {
    private static final String TEMPLATE="Hello %s";
    private final AtomicLong counter= new AtomicLong();

    @GetMapping("/greeting")

    public String greeting(@RequestParam(value="name", defaultValue="World")String name){
        long count=counter.incrementAndGet();
        return String.format(TEMPLATE,name) +"(visits"+count+")";
     
    }



    
}
