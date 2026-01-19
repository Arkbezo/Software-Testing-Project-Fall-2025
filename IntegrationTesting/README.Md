# Integration Testing – POS System

**Author:** Ester Shumeli  
**Course:** Software Testing  
**Testing Level:** Integration Testing  
**Tools:** Java, JUnit 5, MySQL, Eclipse IDE  

---

## Project Overview

This repository contains **integration tests** developed for a Point of Sale (POS) system.  
The goal of the project is to validate the **interaction between multiple system components**, including the Data Access Layer (DAL), Data Transfer Objects (DTOs), and the MySQL database.

All tests are executed using **real implementations and real database operations**, without the use of mocks or stubs.

---

## Scope of Integration Testing

The integration testing covers:
- Communication between DTOs and DAL classes
- Correct execution of SQL queries
- Data persistence and retrieval
- Validation of update, search, and delete flows
- Proper cleanup of test data

---

## Integration Testing Strategies

Different integration strategies are applied depending on the subsystem:

- **Bottom-Up Integration Testing**
- **Big Bang Integration Testing**
- **Sandwich (Hybrid) Integration Testing**






