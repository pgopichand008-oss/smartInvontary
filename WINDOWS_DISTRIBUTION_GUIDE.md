# Windows Distribution & Deployment Guide
## Smart Inventory & Expiry Management System (v1.0.0)

This guide provides instructions for deploying, configuring, and running the **Smart Inventory & Expiry Management System** desktop application on a Windows machine (Windows 10 or Windows 11, 64-bit).

---

## 1. System Requirements

| Requirement | Minimum Specification | Recommended |
| :--- | :--- | :--- |
| **Operating System** | Windows 10 (64-bit) | Windows 11 (64-bit) |
| **Java Runtime (JRE/JDK)** | Java 17 LTS | Java 17 or Java 21 LTS (Eclipse Temurin) |
| **Database Server** | MySQL Server 8.0+ | MySQL Community Server 8.0+ on port 3306 |
| **RAM** | 2 GB available | 4 GB+ |
| **Disk Space** | 100 MB free space | 500 MB free space (for logs & exports) |
| **Display** | 1280 x 720 resolution | 1920 x 1080 (HiDPI auto-scaled by FlatLaf) |

---

## 2. Distribution Package Structure

When distributing the application, the package should contain the following files:

```text
smart-inventory-windows/
├── smart-inventory-system-1.0.0.jar   # Runnable fat JAR (all dependencies bundled)
├── run-app.bat                        # Automated Windows launcher script
├── db.properties.example              # Database configuration template
├── db.properties                      # Active external database configuration
├── schema.sql                         # MySQL database schema & seed data
├── app-icon.ico                       # High-resolution application icon
├── Smart Inventory.lnk                # Windows shortcut with branded icon
└── WINDOWS_DISTRIBUTION_GUIDE.md      # This deployment & troubleshooting guide
```

> [!NOTE]
> Database credentials are **never bundled inside the JAR file**. The application dynamically loads external database connection settings from `db.properties` located in the application directory.

---

## 3. Step-by-Step Installation & Setup

### Step 1: Install Java 17 (or newer)
1. Download **Eclipse Temurin JDK 17 (LTS)** or **JDK 21 (LTS)** from [Adoptium](https://adoptium.net/temurin/releases/).
2. Run the `.msi` installer.
3. On the **Custom Setup** screen, ensure both options are enabled:
   - `Add to PATH` $\rightarrow$ **Will be installed on local hard drive**
   - `Set JAVA_HOME variable` $\rightarrow$ **Will be installed on local hard drive**
4. Complete the installation. Verify by opening a Command Prompt and typing:
   ```cmd
   java -version
   ```

---

### Step 2: Install and Start MySQL Server
1. Download and install **MySQL Community Server 8.0+** (or use an existing instance or XAMPP).
2. Ensure the MySQL service is running. You can verify in PowerShell:
   ```powershell
   Get-Service -Name MySQL*
   ```
   Or via Command Prompt:
   ```cmd
   net start MySQL80
   ```

---

### Step 3: Initialize the Database Schema
1. Open Command Prompt in the application directory.
2. Create the database and import `schema.sql`:
   ```cmd
   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS smart_inventory;"
   mysql -u root -p smart_inventory < schema.sql
   ```
   *(Enter your MySQL root password when prompted).*

This script creates:
- `categories`, `suppliers`, `products`, `product_batches`, `stock_transactions`, `users`, and `app_logs` tables.
- All foreign keys, indexes, and triggers.
- Default administrator and staff accounts.

---

### Step 4: Configure `db.properties`
1. If `db.properties` does not already exist, copy `db.properties.example` to `db.properties`:
   ```cmd
   copy db.properties.example db.properties
   ```
2. Open `db.properties` in Notepad.
3. Update `db.user` and `db.password` with your MySQL credentials:
   ```properties
   db.url=jdbc:mysql://localhost:3306/smart_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
   db.user=root
   db.password=YOUR_MYSQL_PASSWORD_HERE
   ```
4. Save and close the file.

---

### Step 5: Launch the Application
There are three convenient ways to launch the desktop application:

#### Method A: Double-Click the Windows Launcher (Recommended)
Double-click `run-app.bat`.  
The launcher will:
- Check for Java 17+.
- Verify that `db.properties` exists (or auto-create it from template).
- Create a branded Windows shortcut (`Smart Inventory.lnk`) if not already created.
- Launch the application.

#### Method B: Double-Click the Shortcut
Double-click `Smart Inventory.lnk` on your desktop or application folder.

#### Method C: Run from Command Line
```cmd
java -jar smart-inventory-system-1.0.0.jar
```
Or with custom database properties:
```cmd
java -Ddb.config="C:\path\to\my-db.properties" -jar smart-inventory-system-1.0.0.jar
```

---

## 4. Default Seed User Accounts

Once the application launches, the **Login Dialog** will appear. Use any of the pre-configured accounts:

| Username | Password | Full Name | Role | Primary Permissions |
| :--- | :--- | :--- | :---: | :--- |
| `admin` | `admin123` | System Administrator | **ADMIN** | Full administrative access: master data, inventory batches, FEFO stock out, alerts, financial reports, user management. |
| `manager` | `admin123` | Store Manager | **MANAGER** | Operational management: products, categories, suppliers, stock in/out, reports, and alerts. |
| `cashier` | `admin123` | Front Desk Cashier | **CASHIER** | Front desk point-of-sale & inquiries: view catalog, check stock availability, view expiry alerts. Restricted from editing records. |

---

## 5. Troubleshooting & Diagnostics

### 1. "Java Runtime Environment is NOT installed or not in PATH"
- **Cause:** Java is not installed or `java.exe` is not in the system `PATH`.
- **Solution:** Install Eclipse Temurin JDK 17+ from [Adoptium](https://adoptium.net/) and ensure the installer's "Add to PATH" option is checked. Restart any open Command Prompt windows after installing.

### 2. "Database Connection Failed" Dialog on Startup
- **Cause:** Application cannot connect to MySQL on `localhost:3306`.
- **Solution:**
  1. Verify the MySQL service is running via Windows Services (`services.msc` $\rightarrow$ `MySQL80`).
  2. Verify credentials in `db.properties` (`db.user` and `db.password`).
  3. Ensure the database `smart_inventory` was created using `schema.sql`.

### 3. "Access Denied for user 'root'@'localhost'"
- **Cause:** Incorrect password in `db.properties`.
- **Solution:** Open `db.properties` and verify `db.password`. Test logging into MySQL manually using `mysql -u root -p` to confirm the password.

### 4. Application Does Not Open or Window Closes Immediately
- **Cause:** Check console output.
- **Solution:** Run `run-app.bat` from Command Prompt to read the diagnostic messages. The launcher automatically pauses and displays error codes if the application terminates unexpectedly.

---

## 6. Building the Package from Source (Developer Only)

If you have modified the source code and want to re-build the distribution JAR:

1. Ensure Apache Maven 3.8+ and JDK 17+ are installed.
2. Run:
   ```cmd
   mvn clean package -DskipTests
   ```
3. The executable uber-JAR will be generated at:
   `target/smart-inventory-system-1.0.0.jar`
