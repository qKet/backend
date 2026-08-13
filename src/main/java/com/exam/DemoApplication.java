package com.exam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

// @EnableScheduling: 예매 오픈 알림(NotificationServiceImpl.sweepOpenAlerts)이 5분마다 자동 실행되려면 필요
@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("----------------------------------------------------------");
        System.out.println("-----------------------start log -------------------------");
        System.out.println("----------------------------------------------------------");

    }
}
