package com.rental.workflow;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


/**
 * @author gdyang
 */

@ComponentScan(basePackages = {"com.rental.workflow.*"})
@SpringBootApplication
public class RentalWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalWorkflowApplication.class, args);
    }

}
