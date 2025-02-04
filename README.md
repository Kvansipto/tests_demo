Test Automation Project
=============================

This project is a **solution for a test assignment**, with a primary focus on **test coverage and test automation**. The
project demonstrates how to test a service using **API tests, UI tests, parameterized tests, and combined API + UI tests
with parallel execution**.

## 🚀 Features

- ✅ **API Tests**: Validate service logic by directly testing RESTful APIs.
- 🌐 **UI Testing with Selenium**: Automate browser interactions for end-to-end testing of the user interface.
- 🔗 **Combined API + UI Tests**: Tests that combine API interactions and UI verification, simulating real user flows
  with underlying service calls.
- 🔄 **Parallel Test Execution**: Tests are designed to run in multiple threads to speed up execution and optimize
  resources.
- 📊 **Parameterized Tests**: Efficient test execution with different sets of data using JUnit 5.

---

## 🛠️ Technologies Used

- **Kotlin**
- **Gradle**
- **JUnit 5**
- **Selenium WebDriver**

---

## ⚙️ How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/Kvansipto/tests_demo.git
   cd tests_demo
   ```

2. Make sure that the org.gradle.java.home property in gradle.properties is set to the location of Java 17.

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Execute the tests:
   ```bash
   ./gradlew test
   ```

---

Feel free to contribute or provide feedback by opening an issue or submitting a pull request! 😊

---

If you have any questions, feel free to reach out at kvansipto@gmail.com.
