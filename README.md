# SplitWise – Smart Expense Sharing (Java OOP Web Application)

A complete **SplitWise** expense-sharing system built natively in **pure Java** (no external framework/library dependencies) with an interactive modern web frontend.

---

## 🌟 Key Features
- **Group & Member Management**: Create groups and add members dynamically.
- **Smart Expense Splitting**:
  - **Equal Split**: Automatically divides bills equally across members.
  - **Percentage Split**: Divides expenses by custom percentages (with 100% sum validation).
- **Settlement Minimization**: Net-balance greedy algorithm to calculate who owes whom with minimal transaction steps.
- **Settlement & Activity Reports**: Generates detailed settlement breakdown reports in formatted text and JSON.
- **Clean Single-Page Web UI**: Interactive dashboard, member management, expense recording, and real-time debt calculation.

---

## 🏛 Object-Oriented Programming (OOP) Architecture

- **Encapsulation**: Strict private member fields with public accessors and defensive copies across entities (`User`, `Group`, `Expense`, `Settlement`).
- **Inheritance & Polymorphism**:
  - Abstract base class `Expense` with abstract method `calculateSplit(List<User> members)`.
  - Concrete subclasses `EqualExpense` and `PercentageExpense` providing polymorphic split behaviors.
- **Interfaces & Design Patterns**:
  - `SplitStrategy` (Strategy Pattern) implemented by `EqualSplitStrategy` and `PercentageSplitStrategy`.
  - `Reportable` interface implemented by `SettlementReport`.
  - `ExpenseManager` implementing the **Singleton Pattern** managing the central domain state.
- **Built-in Java HTTP Server**: Uses Java's lightweight `com.sun.net.httpserver.HttpServer` with custom REST endpoint routing and static file handling.

---

## 📂 Project Structure

```
splitwise-java/
├── src/
│   └── com/splitwise/
│       ├── Main.java                          # Application entry point
│       ├── model/
│       │   ├── User.java                      # User entity
│       │   ├── Group.java                     # Group entity
│       │   ├── SplitType.java                 # EQUAL, PERCENTAGE enum
│       │   ├── Expense.java                   # Abstract expense base class
│       │   ├── EqualExpense.java              # Concrete equal expense
│       │   ├── PercentageExpense.java         # Concrete percentage expense
│       │   └── Settlement.java                # Settlement transaction model
│       ├── service/
│       │   ├── SplitStrategy.java             # Interface for split strategies
│       │   ├── EqualSplitStrategy.java        # Strategy for equal splitting
│       │   ├── PercentageSplitStrategy.java   # Strategy for percentage splitting
│       │   ├── Reportable.java                # Interface for reporting
│       │   ├── SettlementReport.java          # Settlement report generator
│       │   ├── SettlementCalculator.java      # Debt minimization algorithm
│       │   └── ExpenseManager.java            # Singleton service manager
│       └── server/
│           ├── SplitWiseServer.java           # Embedded HTTP server setup (port 8080)
│           ├── StaticFileHandler.java         # Static file server (serves /web)
│           └── ApiHandler.java                # REST API router & JSON serializer
├── web/
│   ├── index.html                             # Single Page Application
│   ├── style.css                              # Modern styling & theme
│   └── app.js                                 # Frontend state, API client, & dynamic rendering
└── README.md
```

---

## 🚀 How to Run

### Prerequisites
- Java JDK 17+ installed and configured on your system (`javac` and `java` in PATH).

### 1. Compile
From the project root:
```bash
javac -d bin src/com/splitwise/model/*.java src/com/splitwise/service/*.java src/com/splitwise/server/*.java src/com/splitwise/Main.java
```

### 2. Run
```bash
java -cp bin com.splitwise.Main
```

### 3. Open in Browser
Visit [http://localhost:8080](http://localhost:8080) to use the SplitWise web application.
