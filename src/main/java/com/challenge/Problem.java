package com.challenge;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Problem {

    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();

        for (int i = 0; i < 1_000; i++) {
            String role = i % 2 == 0 ? "boss" : "developer";
            String salary = i + ".00";
            String name = "Name" + new SecureRandom().nextInt(100) + i;
            employees.add(new Employee(name, salary, role));
        }

        Instant start = Instant.now();

        saveIntoDatabaseSalariesSum(employees);

        Instant end = Instant.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("Time taken: " + timeElapsed.toMillis() + " milliseconds");

        System.out.println("Boss: " + Database.getInstance().getBossSalary());
        System.out.println("Developer: " + Database.getInstance().getDeveloperSalary());
    }

    /**
     * Employee class
     */
    static class Employee {
        private final String name;
        private final String salary;
        private final String role;

        Employee(String name, String salary, String role) {
            this.name = name;
            this.salary = salary;
            if (!("boss".equals(role) || "developer".equals(role))) {
                throw new RuntimeException("Role must be boss or developer");
            }
            this.role = role;
        }
    }

    /**
     * Single instance with boss and developer salary
     */
    static class Database {
        private static final Database database = new Database();

        private Database() {
        }

        static Database getInstance() {
            return database;
        }

        private double bossSalary = 0.0;
        private double developerSalary = 0.0;

        public double getBossSalary() {
            return bossSalary;
        }

        public void increseBossSalary(double bossSalary) {
            delay();
            this.bossSalary += bossSalary;
        }

        public double getDeveloperSalary() {
            return developerSalary;
        }

        public void increseDeveloperSalary(double developerSalary) {
            delay();
            this.developerSalary += developerSalary;
        }
    }

    /**
     * This method should save into database the sum of salary grouped by role.
     * Note: it should not take more than 3_000 milliseconds to execute 1_000 employees.
     *
     * @param list
     */
    public static void saveIntoDatabaseSalariesSum(List<Employee> list) {

        List<Employee> employeesDuplicated = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        for (int i = 0; i < list.size(); i++) {
            final Employee employee = list.get(i);
            executorService.execute(() -> {
                if (employeesDuplicated.stream().noneMatch(s -> s.name.equals(employee.name))) {
                    if (employee.role.equals("boss")) {
                        Database.getInstance().increseBossSalary(new Double(employee.salary));
                    } else if (employee.role.equals("developer")) {
                        Database.getInstance().increseDeveloperSalary(new Double(employee.salary));
                    }
                    employeesDuplicated.add(employee);
                }
            });
        }

        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error running tasks", e);
        }
    }


    private static void delay() {
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
