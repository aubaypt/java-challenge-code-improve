package com.challenge;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class AlternativeSolution {

  public static void main(String[] args) {
    List<Employee> employees = new ArrayList<>();

    for (int i = 0; i < 1_000; i++) {
      String role = i % 2 == 0 ? "boss" : "developer";
      String salary = i + ".00";
      String name = "Name" + new SecureRandom().nextInt(100) + "_" + i;
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

  enum Role {
    BOSS("boss"),
    DEVELOPER("developer");

    private final String description;

    Role(String description) {

      this.description = description;
    }

    public static Role getByDescription(String description) {
      return Stream.of(values())
          .filter(role -> role.description.equalsIgnoreCase(description))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("This role doesn't exist"));
    }

    public boolean isBoss() {
      return this == BOSS;
    }

    public boolean isDeveloper() {
      return this == DEVELOPER;
    }
  }

  static class Employee {

    private final String name;
    private final BigDecimal salary;
    private final Role role;

    Employee(String name, String salary, String role) {
      this.name = Objects.requireNonNull(name);
      this.salary = new BigDecimal(Objects.requireNonNull(salary));
      this.role = Role.getByDescription(role);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (!(o instanceof Employee employee)) {
        return false;
      }

      return name.equals(employee.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }
  }

  /**
   * Singleton database with boss and developer salary
   */
  static class Database {

    private Database() {
    }

    static Database getInstance() {
      return DatabaseHolder.database;
    }

    private static final class DatabaseHolder {

      private static final Database database = new Database();
    }

    private final AtomicReference<BigDecimal> bossSalary = new AtomicReference<>(BigDecimal.ZERO);
    private final AtomicReference<BigDecimal> developerSalary = new AtomicReference<>(BigDecimal.ZERO);

    public BigDecimal getBossSalary() {
      return bossSalary.get();
    }

    public void increaseBossSalary(BigDecimal bossSalary) {
      delay();
      this.bossSalary.accumulateAndGet(bossSalary, BigDecimal::add);
    }

    public BigDecimal getDeveloperSalary() {
      return developerSalary.get();
    }

    public void increaseDeveloperSalary(BigDecimal developerSalary) {
      delay();
      this.developerSalary.accumulateAndGet(developerSalary, BigDecimal::add);
    }
  }

  /**
   * This method should save into database the sum of salary grouped by role. Note: it should not
   * take more than 3_000 milliseconds to execute 1_000 employees.
   *
   * @param list
   */
  public static void saveIntoDatabaseSalariesSum(List<Employee> list) {

    Set<Employee> employees = ConcurrentHashMap.newKeySet(list.size());
    employees.addAll(list);

    try {
      ExecutorService executorService = Executors.newFixedThreadPool(2);

      executorService.execute(() -> employees
          .parallelStream()
          .filter(employee -> employee.role.isBoss())
          .map(employee -> employee.salary)
          .forEach(Database.getInstance()::increaseBossSalary));

      executorService.execute(() -> employees
          .parallelStream()
          .filter(employee -> employee.role.isDeveloper())
          .map(employee -> employee.salary)
          .forEach(Database.getInstance()::increaseDeveloperSalary));

      executorService.shutdown();
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
