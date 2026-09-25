# Real-Time Air Quality Monitoring System
### Core Java Servlets (Jakarta EE 10) + JDBC + MySQL + OpenWeatherMap API

This project contains the complete, production-ready implementation designed to be built, deployed, and run entirely using **Visual Studio Code** and **Apache Tomcat 10+**.

---

## Prerequisites & Installation Guide for VS Code

### 1. Java Development Kit (JDK)
- Install **JDK 17** or **JDK 21** (e.g. Eclipse Temurin or Oracle JDK).
- Check installation in terminal:
  ```bash
  java -version
  javac -version
  ```
- Ensure `JAVA_HOME` environment variable points to your JDK directory.

### 2. VS Code Extensions
Open VS Code and install the following extensions from the Marketplace:
1. **Extension Pack for Java** (by Microsoft) — includes Java Language Support, Debugger, Maven for Java, Project Manager.
2. **Community Server Connectors** (by Red Hat) OR **Tomcat for Java** — allows adding, starting, and debugging Apache Tomcat directly inside VS Code.
3. **MySQL** (by Weijan Chen) or **Database Client** (optional for viewing MySQL directly in VS Code).

### 3. Apache Tomcat 10+
- Download **Apache Tomcat 10.1.x** (zip or tar.gz) from [tomcat.apache.org](https://tomcat.apache.org/download-10.cgi).
- Extract it to a folder, e.g., `C:\apache-tomcat-10.1.x` (Windows) or `/opt/tomcat10` (macOS/Linux).
- Note: Tomcat 10+ uses `jakarta.servlet.*` which is what this codebase uses.

### 4. MySQL Setup
1. Start MySQL Server.
2. Open MySQL CLI or MySQL Workbench:
   ```sql
   source /path/to/AirQualityMonitor/database.sql;
   ```
   Or execute the SQL commands in `database.sql`:
   ```sql
   CREATE DATABASE IF NOT EXISTS air_quality_db;
   USE air_quality_db;
   -- Creates users and air_quality_history tables
   ```
3. Open `src/main/java/com/airquality/DBConnection.java` and enter your MySQL root password:
   ```java
   private static final String DB_PASSWORD = "YOUR_MYSQL_PASSWORD";
   ```

---

## How to Build and Run in VS Code

### Step 1: Open Project in VS Code
Open the `AirQualityMonitor` folder in VS Code (`File > Open Folder...`).

### Step 2: Build the WAR with Maven
Open VS Code integrated terminal (`Ctrl + \`` or `Cmd + \``):
```bash
mvn clean package
```
This generates `target/AirQualityMonitor.war`.

### Step 3: Configure and Start Apache Tomcat in VS Code
1. In the VS Code Explorer sidebar, expand **Servers** (provided by Community Server Connectors) or use the Tomcat extension.
2. Click **Create New Server** > select **Apache Tomcat** > choose your extracted Tomcat 10 directory.
3. Right-click the configured Tomcat server > select **Add Deployment** > select the `AirQualityMonitor` project or `target/AirQualityMonitor.war`.
4. Right-click the server > **Start Server**.

*Alternative Manual Deployment:*
Simply copy `target/AirQualityMonitor.war` into your Tomcat `webapps/` folder, then run:
- Windows: `bin\startup.bat`
- Mac/Linux: `bin/startup.sh`

### Step 4: Open in Browser
Navigate to:
```
http://localhost:8080/AirQualityMonitor/login.html
```

---

## Application Flow

1. **Register**: Go to `register.html` > Enter name, username, email, password > User saved with BCrypt in MySQL > Redirects to `login.html`.
2. **Login**: Enter username/email and password > Validated via `LoginServlet` > `HttpSession` created > Redirects to `air.html`.
3. **Session Check**: `air.html` immediately verifies session with `AuthCheckServlet`. If unauthenticated, redirects to `login.html`.
4. **Air Quality Search**: Enter city > OpenWeatherMap Geocoding & Air Pollution APIs fetch data > Displays AQI, health alert, and 8 pollutants > `SaveAirQualityServlet` saves metrics to MySQL for the logged-in user.
5. **Search History**: Click `Search History` > `history.html` queries `HistoryServlet` > Displays only the logged-in user's records.
6. **Logout**: Click `Logout` > `LogoutServlet` invalidates `HttpSession` > Redirects to `login.html`.
