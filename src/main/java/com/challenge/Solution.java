package com.challenge;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Solution {

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
        private final Role role;

        Employee(String name, String salary, String role) {
            this.name = name;
            this.salary = salary;
            this.role = Role.getByDescriptionOrException(role);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Employee employee = (Employee) o;
            return Objects.equals(name, employee.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        public String getName() {
            return name;
        }

        public String getSalary() {
            return salary;
        }

        public Role getRole() {
            return role;
        }
    }

    enum Role {
        BOSS("boss"),
        DEVELOPER("developer");

        private final String description;

        Role(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static Role getByDescriptionOrException(String description) {
            return Arrays.stream(values()).filter(r -> r.getDescription().equals(description))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Role must be boss or developer"));
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

        private BigDecimal bossSalary = BigDecimal.ZERO;
        private BigDecimal developerSalary = BigDecimal.ZERO;

        public BigDecimal getBossSalary() {
            return bossSalary;
        }

        public synchronized void increseBossSalary(BigDecimal bossSalary) {
            delay();
            this.bossSalary = this.bossSalary.add(bossSalary);
        }

        public BigDecimal getDeveloperSalary() {
            return developerSalary;
        }

        public void increseDeveloperSalary(BigDecimal developerSalary) {
            delay();
            this.developerSalary = this.developerSalary.add(developerSalary);
        }
    }

    /**
     * This method should save into database the sum of salary grouped by role.
     * Note: it should not take more than 1_000 milliseconds to execute 1_000 employees.
     *
     * @param list
     */
    public static void saveIntoDatabaseSalariesSum(List<Employee> list) {
        new HashSet<>(list)
                .parallelStream()
                .forEach(employee -> {
                    switch (employee.getRole()) {
                        case BOSS -> Database.getInstance().increseBossSalary(new BigDecimal(employee.getSalary()));
                        case DEVELOPER -> Database.getInstance().increseDeveloperSalary(new BigDecimal(employee.getSalary()));
                        default ->
                                throw new UnsupportedOperationException("Is not possible to calculate the salary from role: " + employee.getRole());
                    }
                });
    }


    private static void delay() {
        try {
            Thread.sleep(3);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
