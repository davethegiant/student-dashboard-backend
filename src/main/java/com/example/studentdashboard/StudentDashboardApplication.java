package com.example.studentdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class StudentDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentDashboardApplication.class, args);
    }
}
