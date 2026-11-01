# Smart Inventory & Expiry Management System

Desktop-based Inventory & Expiry Management System developed with **Java Swing**, **JDBC**, and **MySQL**.

---

## Architecture Overview
The application strictly enforces a **4-tier architecture**:
```
Swing UI (FlatLaf) ──► Controller Layer ──► Service Layer (Validation & Business Rules) ──► DAO Layer (PreparedStatements) ──► HikariCP ──► MySQL
```

---

## Phase 1: Database Setup & Foundations (Complete)
- **Database**: MySQL 8.x (`smart_inventory`) with normalized tables: `users`, `categories`, `suppliers`, `products`, `product_batches`, `stock_transactions`.
- **Connection Pool**: HikariCP connection pool configured in [`DatabaseConnection.java`](src/main/java/com/smartinventory/config/DatabaseConnection.java).
- **Credentials**: Stored securely in [`src/main/resources/db.properties`](src/main/resources/db.properties) (git-ignored).

---

## Phase 2: Categories and Suppliers Management (Complete)
- **Category Management**: Full CRUD, validation (2–100 chars), duplicate prevention, product-linked deletion protection.
- **Supplier Directory**: Full CRUD, phone & email validation, duplicate prevention, active status toggling.
- **Views**: [`CategoryManagementPanel`](src/main/java/com/smartinventory/view/category/CategoryManagementPanel.java) and [`SupplierManagementPanel`](src/main/java/com/smartinventory/view/supplier/SupplierManagementPanel.java).

---

## Phase 3: Base UI Shell & Authentication (Complete)

### Features Implemented
1. **Security & Cryptography**:
   - [`PasswordUtil`](src/main/java/com/smartinventory/util/PasswordUtil.java): BCrypt password hashing (work factor 12) and constant-time hash verification.
2. **Session Management**:
   - [`UserSession`](src/main/java/com/smartinventory/util/UserSession.java): Thread-safe singleton tracking authenticated operator, session listeners, and role checks (`isAdmin()`, `isManagerOrAdmin()`).
3. **Authentication Engine**:
   - [`UserDAO`](src/main/java/com/smartinventory/dao/UserDAO.java): Parameterized queries for user retrieval, creation, and password updates.
   - [`AuthService`](src/main/java/com/smartinventory/service/AuthService.java): Secure login validation, account deactivation guard, and logout handling.
   - [`AuthController`](src/main/java/com/smartinventory/controller/AuthController.java): Asynchronous `SwingWorker` authentication preventing UI freezing.
4. **Modern UI Shell**:
   - [`LoginDialog`](src/main/java/com/smartinventory/view/auth/LoginDialog.java): FlatLaf login dialog with revealable password field and inline error messages. Default Admin: `admin` / `admin123`.
   - [`SidebarPanel`](src/main/java/com/smartinventory/view/layout/SidebarPanel.java): Role-aware vertical navigation with active highlighting, user profile badge, and sign-out action.
   - [`MainFrame`](src/main/java/com/smartinventory/view/layout/MainFrame.java): Core desktop shell embedding Category & Supplier views inside a CardLayout with placeholder cards for future phases.

---

## Phase 4: Product Catalog Management (Complete)
- **Product Domain Model**: [`Product.java`](src/main/java/com/smartinventory/model/Product.java) encapsulating SKU, Barcode, Category ID/Name, Unit of Measure, Minimum Stock Alert Threshold, and Active state.
- **Data Access Layer**: [`ProductDAO.java`](src/main/java/com/smartinventory/dao/ProductDAO.java) executing parameterized queries with Category JOINs, case-insensitive SKU uniqueness, optional barcode deduplication, and relational batch guards.
- **Service Layer**: [`ProductService.java`](src/main/java/com/smartinventory/service/ProductService.java) enforcing positive minimum stock constraints, category existence verification, duplicate prevention, and status toggling.
- **Controller Layer**: [`ProductController.java`](src/main/java/com/smartinventory/controller/ProductController.java) managing UI interactions asynchronously via `SwingWorker`.
- **UI Components**:
  - [`ProductManagementPanel`](src/main/java/com/smartinventory/view/product/ProductManagementPanel.java): Live search, category and status filtering, table display with pill badges, and CRUD controls.
  - [`ProductFormDialog`](src/main/java/com/smartinventory/view/product/ProductFormDialog.java): Modal dialog for product creation and editing with category selection, SKU auto-generation, units, and min stock spinner.
- **Tests**: Comprehensive JUnit 5 test suite [`ProductServiceTest.java`](src/test/java/com/smartinventory/service/ProductServiceTest.java) covering lifecycle, validations, duplicate handling, and filtering.

---

## Phase 5: Batch Tracking & Inventory Operations (Complete)
- **Multi-Batch Model & Domain Entities**:
  - [`Batch.java`](src/main/java/com/smartinventory/model/Batch.java): Physical batch lots tracking batch number, purchase/selling price, initial/current quantities, manufacturing & expiry dates, received date, and [`BatchStatus`](src/main/java/com/smartinventory/model/enums/BatchStatus.java).
  - [`StockTransaction.java`](src/main/java/com/smartinventory/model/StockTransaction.java): Immutable ledger movement entries with [`TransactionType`](src/main/java/com/smartinventory/model/enums/TransactionType.java) (`PURCHASE_IN`, `SALE_OUT`, `RETURN_IN`, `DAMAGE_OUT`, `EXPIRED_WRITE_OFF`).
  - [`StockAllocation.java`](src/main/java/com/smartinventory/model/StockAllocation.java) & [`StockOutResult.java`](src/main/java/com/smartinventory/model/StockOutResult.java): Value objects summarizing multi-batch FEFO deductions.
- **Data Access Layer**:
  - [`BatchDAO.java`](src/main/java/com/smartinventory/dao/BatchDAO.java): FEFO row query with `FOR UPDATE` pessimistic locking, aggregate stock calculations, and dynamic search filters.
  - [`StockTransactionDAO.java`](src/main/java/com/smartinventory/dao/StockTransactionDAO.java): Recording immutable stock movement ledger records.
- **Service Layer**:
  - [`InventoryService.java`](src/main/java/com/smartinventory/service/InventoryService.java): Enforcing atomic SQL transactions with rollback, price sanity checks (`sellingPrice >= purchasePrice`), FEFO allocation algorithm (earliest expiring batches consumed first), and [`InsufficientStockException`](src/main/java/com/smartinventory/exception/InsufficientStockException.java).
- **Controller Layer**:
  - [`InventoryController.java`](src/main/java/com/smartinventory/controller/InventoryController.java): Managing UI operations asynchronously via `SwingWorker`.
- **UI Components**:
  - [`InventoryManagementPanel`](src/main/java/com/smartinventory/view/inventory/InventoryManagementPanel.java): Tabbed view hosting Batch Inventory and Stock Movement Ledger with search, filters, and color badges.
  - [`StockInDialog`](src/main/java/com/smartinventory/view/inventory/StockInDialog.java): Modal for receiving new batch lots with auto-batch numbering, pricing, and expiration dates.
  - [`StockOutDialog`](src/main/java/com/smartinventory/view/inventory/StockOutDialog.java): Modal for FEFO stock dispatch with live available stock queries and multi-batch allocation summary.
  - [`InventoryStatusBadgeRenderer`](src/main/java/com/smartinventory/view/components/InventoryStatusBadgeRenderer.java): Color-coded badges for batch statuses and movement types.
- **Automated Tests**:
  - [`InventoryServiceTest.java`](src/test/java/com/smartinventory/service/InventoryServiceTest.java): Testing Stock In, multi-batch support, FEFO allocation order, multi-batch order spanning with depletion, insufficient stock rejection, validation constraints, and transaction ledger audit trails.

---

## Phase 6: Expiry Date Engine & Low-Stock Alert Center (Complete)
- **Surveillance Entities & Enums**:
  - [`ExpiryAlert.java`](src/main/java/com/smartinventory/model/ExpiryAlert.java): Domain entity for expired and expiring lots with accurate DATEDIFF day calculations and [`ExpirySeverity`](src/main/java/com/smartinventory/model/enums/ExpirySeverity.java) (`EXPIRED`, `EXPIRING_7_DAYS`, `EXPIRING_15_DAYS`, `EXPIRING_30_DAYS`).
  - [`LowStockAlert.java`](src/main/java/com/smartinventory/model/LowStockAlert.java): Domain entity tracking stock deficits against `min_stock_level` with [`StockAlertSeverity`](src/main/java/com/smartinventory/model/enums/StockAlertSeverity.java) (`OUT_OF_STOCK`, `LOW_STOCK`).
  - [`AlertSummary.java`](src/main/java/com/smartinventory/model/AlertSummary.java): Aggregated KPI counts and lists for dashboard overview.
- **Data Access Layer**:
  - [`AlertDAO.java`](src/main/java/com/smartinventory/dao/AlertDAO.java): Complex queries for expired lots, upcoming expiration horizons (7, 15, 30 days), eligible unexpired low-stock calculations, exclusion of inactive items/depleted lots, and database status synchronization.
- **Service Layer**:
  - [`AlertService.java`](src/main/java/com/smartinventory/service/AlertService.java): Automated status synchronization and listener event subscription.
- **Controller Layer**:
  - [`AlertController.java`](src/main/java/com/smartinventory/controller/AlertController.java): Asynchronous `SwingWorker` queries for alert cards and tables.
- **UI Components**:
  - [`AlertsPanel.java`](src/main/java/com/smartinventory/view/alert/AlertsPanel.java): KPI metric tiles (Expired, Expiring Soon, Out of Stock, Low Stock), category & horizon filters, and 3 surveillance tabs with color badges.
  - [`AlertBadgeRenderer.java`](src/main/java/com/smartinventory/view/components/AlertBadgeRenderer.java): Pill badges for critical, warning, out-of-stock, and low-stock severities.
  - [`MainFrame.java`](src/main/java/com/smartinventory/view/layout/MainFrame.java): Wired to `SidebarPanel.NAV_ALERTS` with automatic event-driven refresh whenever stock movements occur.
- **Automated Tests**:
  - [`AlertServiceTest.java`](src/test/java/com/smartinventory/service/AlertServiceTest.java): Testing expired lot detection, 7/15/30 day boundaries, low-stock deficits, out-of-stock items, inactive product exclusion, and depleted batch exclusion.

---

## Phase 7: Dashboard, Analytics & Reporting (Complete)
- **Executive Dashboard Metrics & Analytics**:
  - [`DashboardMetrics.java`](src/main/java/com/smartinventory/model/dashboard/DashboardMetrics.java): Aggregating 6 core inventory KPIs: Total Products, Physical Stock On Hand, Inventory Cost & Retail Potential Valuations, Master Taxonomy Counts (Categories & Suppliers), Expiration Risk Counts (Expired + ≤30d Expiring), and Reorder Deficit Counts (Low-Stock + Out-of-Stock).
  - [`CategoryStockStat.java`](src/main/java/com/smartinventory/model/dashboard/CategoryStockStat.java): Breakdown of physical units and distribution percentage by product category.
  - [`MonthlyMovementStat.java`](src/main/java/com/smartinventory/model/dashboard/MonthlyMovementStat.java): Monthly movement trends aggregating Purchases/Inflow, Sales/Outflow, and Discards/Losses.
- **Data Access & Analytics Layer**:
  - [`DashboardDAO.java`](src/main/java/com/smartinventory/dao/DashboardDAO.java): Complex queries with Category-wise aggregations, cost/retail potential calculations, and rolling monthly transaction breakdowns.
- **Reporting Engine & Models**:
  - [`ReportType.java`](src/main/java/com/smartinventory/model/report/ReportType.java): Enum covering `INVENTORY_VALUATION`, `EXPIRY_SURVEILLANCE`, `LOW_STOCK_REORDER`, and `TRANSACTION_AUDIT`.
  - [`ReportData.java`](src/main/java/com/smartinventory/model/report/ReportData.java): Tabular report model with dynamic headers, row data, metadata, and summary analytics notes.
  - [`ReportService.java`](src/main/java/com/smartinventory/service/ReportService.java): Compiles detailed reports for all 4 report types from live MySQL tables.
- **Zero-Dependency Pure-Java Export Engines**:
  - [`PdfReportExporter.java`](src/main/java/com/smartinventory/util/export/PdfReportExporter.java): Pure Java standards-compliant PDF-1.4 writer with styled corporate headers, data table grid, alternating zebra striping, pagination, and summary notes without external library bloat.
  - [`ExcelReportExporter.java`](src/main/java/com/smartinventory/util/export/ExcelReportExporter.java): Pure Java Microsoft Office SpreadsheetML (`.xls`) writer preserving typography, colors, header styles, and cell typing.
  - [`CsvReportExporter.java`](src/main/java/com/smartinventory/util/export/CsvReportExporter.java): Standard CSV generator with UTF-8 Byte Order Mark (BOM) for native Excel compatibility.
- **Interactive Swing UI & Graphics2D Charts**:
  - [`CategoryDistributionChartPanel.java`](src/main/java/com/smartinventory/view/dashboard/CategoryDistributionChartPanel.java): Modern anti-aliased Donut Chart with center total units counter and category percentage legend.
  - [`MonthlyMovementChartPanel.java`](src/main/java/com/smartinventory/view/dashboard/MonthlyMovementChartPanel.java): Modern anti-aliased Grouped Bar Chart with horizontal value grid, color-coded series (Inflow, Outflow, Discards), and month labels.
  - [`DashboardPanel.java`](src/main/java/com/smartinventory/view/dashboard/DashboardPanel.java): Modern executive overview with 6 metric cards, responsive split charts, quick jump actions, role-gated financial valuation visibility, and auto-refresh on stock transactions.
  - [`ReportManagementPanel.java`](src/main/java/com/smartinventory/view/report/ReportManagementPanel.java): Interactive reporting suite with dynamic filter controls (Category, Horizon, Transaction Type, Limit), tabular preview table, summary notes card, and one-click PDF, Excel, and CSV file exports with overwrite guards.
- **Main Shell Integration**:
  - [`MainFrame.java`](src/main/java/com/smartinventory/view/layout/MainFrame.java): Full replacement of all placeholders; wires `DashboardPanel` and `ReportManagementPanel` to sidebar navigation and inventory listeners.
- **Automated Tests**:
  - [`DashboardServiceTest.java`](src/test/java/com/smartinventory/service/DashboardServiceTest.java): Automated verification of real database KPI aggregation, non-negative bounds, category percentage distribution, and monthly movement stats.
  - [`ReportServiceTest.java`](src/test/java/com/smartinventory/service/ReportServiceTest.java): Automated verification of all 4 report generation types, valid PDF-1.4 binary structure (`%PDF-1.4` and `%%EOF`), SpreadsheetML XML structure, and UTF-8 BOM CSV output.

---

## Application Branding & Desktop Icon
- **Branding Assets**:
  - `src/main/resources/icons/app-icon.png`: Master high-resolution brand icon (1024x819 PNG).
  - `src/main/resources/icons/app-icon.ico` & `app-icon.ico`: Multi-resolution Windows ICO (16x16, 24x24, 32x32, 48x48, 64x64, 128x128, 256x256).
- **Utility & Window Integration**:
  - [`AppIconUtil.java`](src/main/java/com/smartinventory/util/AppIconUtil.java): Loads icon from classpath, generates multi-resolution images for OS scaling, applies icon list to `MainFrame` and `LoginDialog`, and configures `java.awt.Taskbar` icon.
  - Modern branded visual integration inside `LoginDialog` (64x64 header) and `SidebarPanel` (28x28 logo).
- **Windows Launcher**:
  - `run-app.bat`: Configured with automatic generation of branded Windows shortcut `Smart Inventory.lnk` with embedded icon metadata.

---

## Running the Application & Tests

### Database Configuration
Ensure MySQL 8.x is running and configured in `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/smart_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=your_password
db.pool.maxPoolSize=10
db.pool.minIdle=2
```

### Run All 57 Automated Unit & Integration Tests
```bash
mvn test
```

### Launch the Complete Desktop Application
```bash
mvn exec:java -Dexec.mainClass="com.smartinventory.Main"
```
Or double-click:
```bash
run-app.bat
```
Or open the branded Windows shortcut:
```bash
Smart Inventory.lnk
```

### Default Credentials
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: `ADMIN` (Full privileges: Master Data, Batches, Stock In/Out, Alert Center, Analytics, Reports & Financial Valuation)
