# Project Changelog

All notable changes, milestones, refactoring phases, and documentation updates for the Travel Booking System repository are documented chronologically in this file.

---

## [2026-11-08] - Salted SHA-256 Password Cryptography & Security Hardening

### Added
- **`PasswordUtil.java` (`service/`):** Implemented cryptographic password security using 16-byte random salt generation (`SecureRandom`) and SHA-256 message digests (`MessageDigest`). Standard format: `<saltHex>$<hashHex>`.
- **Constant-Time Verification:** Used `MessageDigest.isEqual` to prevent side-channel timing attacks during password authentication.
- **Legacy Fallback Compatibility:** Built-in verification fallback support for un-salted plain-text seed records (e.g., `admin123`) and raw SHA-256 digests.
- **`PasswordUtilTest.java`:** Added 6 comprehensive JUnit 5 unit tests covering salt randomness, verification success/failure, input validation, and legacy fallback support.

### Changed
- **`LoginFrame.java`:** Secured user registration and authentication by hashing passwords before persisting to database and caching in `CustomerHashTable`, and verifying via `PasswordUtil.verifyPassword(...)`.
- **`CustomerRegistryService.java`:** Integrated `PasswordUtil` into `registerCustomer(...)` and `authenticate(...)`.
- **`database/01_admin.sql`:** Expanded `users.password` column definition from `VARCHAR(50)` to `VARCHAR(255)` to prevent truncation of salted cryptographic digests.

---

## [2026-11-06] - Automated JavaDoc Deployment & Admin-Only Report Access

### Added
- **Systemd Automation (`systemd/`):** Added a systemd service and timer on the local Omarchy server to automate JavaDoc generation and GitHub Pages deployment for `dsa_travel_booking_system-nightly` and its downstream mirror `dsa_travel_booking_system-beta`.
- **GitHub Pages Publishing (`gh-pages`):** Configured an orphaned `gh-pages` branch in both repositories to host the generated API documentation.
- **Admin-Only Report Access (`BookingPanel.java`):** Restricted the `Report` button on the `Bookings & Itinerary` tab so it is visible and actionable only for accounts with the `ADMIN` role.

### Changed
- **Repository Naming:** Renamed the projects to `dsa_travel_booking_system-nightly` and `dsa_travel_booking_system-beta`, and updated the corresponding `pom.xml` `<artifactId>` values to match.

### Fixed
- **`database/02_customers.sql`:** Confirmed removal of the operational sample-data script; only `database/01_admin.sql` remains, per the admin-only seeding rule.

## [2026-11-03] - Service and GUI Reflection Decoupling Unit Tests

### Added
- **`FlightCatalogServiceTest.java`:** Added JUnit 5 unit tests verifying that in-memory flight lists are sorted chronologically via reflected `ds.FlightBST`.
- **`CustomerRegistryServiceTest.java`:** Added JUnit 5 unit tests verifying user caching and retrieval via reflected `ds.CustomerHashTable`.
- **`PackagePanelReflectionTest.java`:** Added JUnit 5 unit tests exercising `PackagePanel` reflection integration with `ds.PackageQuickSort` for price and rating sorting.
- **`FlightPanelReflectionTest.java`:** Added JUnit 5 unit tests exercising `FlightPanel` reflection integration with `ds.FlightBST`.

### Changed
- **`FlightCatalogService.java`:** Added `sortFlightsWithBST(List<Flight>)` to decouple reflection execution from JDBC and enable direct unit testing.
- **`CustomerRegistryService.java`:** Exposed `cacheUser(User)` and `getCachedUser(String)` methods to enable direct unit testing of reflected hash table caching without active database connectivity.
- **`PackagePanel.java`:** Extracted static `sortPackagesWithReflection(List<PackageDeal>, String, boolean)` to enable direct unit testing of QuickSort reflection.
- **`FlightPanel.java`:** Extracted static `sortFlightsWithReflection(List<Flight>)` to enable direct unit testing of FlightBST reflection.

---

## [2026-11-03] - Item 9 Compliance Audit and Refactor

### Added
- **`CustomerHashTable.java`:** Added the canonical Item 9 separate-chaining customer hash table API.
- **`PackageQuickSort.java`:** Added the canonical Item 9 hand-written package sorting API with price and rating direction toggles.
- **Booking Update Workflow:** Added `Update Selected` with booking row auto-fill and recalculated totals.
- **JUnit 5 Tests:** Added focused tests for customer hash-table CRUD/collisions and package QuickSort ordering.

### Changed
- **Service Reflection:** Updated flight and customer services to use reflection for optional DSA integration with JDBC fallback behavior.
- **Compatibility:** Retained `UserHashTable` and `QuickSort` as compatibility aliases for existing callers.
- **`pom.xml`:** Added the required Java 21 compiler, Surefire, and Exec Maven plugin configuration.
- **Database Seeding:** Removed the forbidden operational sample-data script so only the admin seed remains.
- **Documentation:** Updated `README.md` to reflect the authoritative Item 9 architecture and live-seeding rule.

### Fixed
- **`QuickSort.sortHotelsByPrice(...)`:** Replaced the unfinished `UnsupportedOperationException` stub with a working sort implementation.

## [2026-10-30] - Master Prompt Blueprint Refinement & Multi-Project LLM Generation Guide

### Added
- **LLM Adaptation & Generation Guide (`PROMPT_GUIDE.md`):**
  - Created a 1-step prompt template for Google Gemini, ChatGPT, and Claude to instantly generate complete, compliant implementations for any project item (Items 1–8 and Item 10).
  - Included quick copy-paste prompts for all project domains with verification checklists.
- **Refined Master Framework Blueprint (`master_prompt.md`):**
  - Expanded Section 3 with exhaustive entity properties, custom data structures (BST, Hash Tables, Priority Queues, Linked Lists, Graphs), service methods, and GUI specifications across all 9 domain items.
  - Standardized the non-negotiable 5-tier architecture (`model`, `ds`, `service`, `database`, `gui`) and CRUD update standards.

---

## [2026-10-30] - GUI Layout Dimension Expansion & Button Spacing Refinement

### Changed
- **Expanded Main Dashboard Dimensions:**
  - Increased `MainFrame` window size from `850x700` to `960x720` to prevent component squishing and label truncations.
  - Expanded inner `tabbedPane` bounds from `790x580` to `900x610` with repositioned Logout button (`810, 10`).
- **Refined Panel Bounds & Button Layouts:**
  - In `BookingPanel.java`, resized and spaced action buttons (`Book Package`, `View Itinerary`, `Invoice`, `Report`, `Cancel`) with clean margins and widened table bounds (`875x520`).
  - In `FlightPanel.java`, `HotelPanel.java`, and `PackagePanel.java`, expanded panel bounds to `900x580` and widened tables to `875px`.

---

## [2026-10-30] - Full CRUD Updates, Service Layer Architecture, Priority Queue & Executive Reports

### Added
- **Complete End-to-End Update Operations ("U" in CRUD):**
  - Added "Update Selected" buttons and table row selection listeners in `FlightPanel.java` and `HotelPanel.java`.
  - Updating a flight or hotel updates MySQL and automatically re-syncs dynamic package deals across panels.
- **Dedicated Service Layer Architecture (`service/`):**
  - Implemented `FlightCatalogService.java` for coordinating flight persistence and BST sorting.
  - Implemented `CustomerRegistryService.java` for user management and caching via `UserHashTable`.
  - Implemented `BookingManagementService.java` for hotel CRUD, booking workflows, and priority queue ordering.
- **`BookingPriorityQueue` (Priority Queue DSA) & Unit Tests:**
  - Implemented `ds.BookingPriorityQueue` (heap-backed transaction ordering) with comprehensive JUnit 5 tests (`BookingPriorityQueueTest.java`).
- **Executive System Summary Reporting:**
  - Added on-demand revenue and customer booking activity reporting accessible via a "Report" button on `BookingPanel.java`.
- **Database Self-Refreshing Drop Scripts:**
  - Updated `database/01_admin.sql` to safely drop existing tables in reverse foreign key order (`bookings -> hotels -> flights -> users`) before recreating them to cleanly prepare for `database/02_customers.sql`.

---

## [2026-10-30] - Quality Assurance, Dynamic Package Synchronization & Hash Table Enhancements

### Added
- **`UserHashTable.remove(String key)` Implementation & Unit Tests:**
  - Implemented node deletion with pointer re-linking for separate chaining in `ds.UserHashTable`.
  - Added unit test `testRemove()` in `ds.UserHashTableTest` covering single node deletion, preservation of neighboring chained nodes, and safe handling of non-existent keys.
- **Dynamic Cross-Panel Synchronization:**
  - Added `refreshPackages()` to `PackagePanel.java` and exposed `refreshPackagePanel()` in `MainFrame.java`.
  - Admin addition and removal operations in both `FlightPanel.java` and `HotelPanel.java` now automatically trigger package deal re-pairing and re-sorting without requiring application restarts.

### Changed
- **Maven Configuration (`pom.xml` & `nbactions.xml`):**
  - Updated `<exec.mainClass>` to point directly to `Main` instead of the outdated subpackage path, ensuring seamless launch via `mvn exec:java`.
- **Documentation Updates:**
  - Updated `README.md` and `master_prompt.md` to document dynamic panel event notification, hashtable deletion capabilities, and clean application launch specifications.

---

## [2026-10-30] - Paired Military Times, Root Database Scripts & Layout Refinements

### Added
- **Paired Military Departure & Arrival Times (`HH:mm`):**
  - Updated `flights` schema with paired `departure_time` (`VARCHAR(5)`) and `arrival_time` (`VARCHAR(5)`).
  - Updated `FlightPanel.java` input layout to pair Departure Time right next to Departure Location and Arrival Time right next to Arrival Location.
  - Added strict regex validation (`^([01]\d|2[0-3]):([0-5]\d)$`) for both time fields.
  - Updated `tblFlights` table headers to `["ID", "Departure", "Dep Time", "Arrival", "Arr Time", "Price"]`.
- **Root Database Seed Scripts (`database/`):**
  - Created `database/01_admin.sql` initializing database schema and pre-seeding default admin account (`admin` / `admin123`).
  - Created `database/02_customers.sql` inserting exactly 10 customer accounts and 20 sample flight-hotel bookings.
- **Smart Destination-Matching Package Deals:**
  - Implemented `PackageDeal.normalizeDestination()` and `PackageDeal.isDestinationMatch()` to pair flights and hotels strictly when destinations match, ignoring airport codes or parenthetical indicators (e.g. Flight arrival `"Tokyo (NRT)"` matches Hotel location `"Tokyo"`).
- **Toggleable QuickSort with Dynamic Directional Indicators:**
  - Refactored `QuickSort.java` to support both Ascending and Descending sorting modes.
  - Updated `PackagePanel.java` buttons (`btnSortPrice` and `btnSortRating`) with dynamic directional indicators (`"Sort Price (Ascending ↑)"` vs `"Sort Price (Descending ↓)"`), enforcing exclusive active indicator state clearing.

### Changed
- **Unit Test Suite Updates:**
  - Updated `ds/FlightBSTTest.java` to cover paired departure and arrival military times and chronological departure sorting.
  - Updated `ds/QuickSortTest.java` and `model/PackageDealTest.java` to verify toggleable sorting modes and smart destination normalization.
- **Documentation Updates:**
  - Updated `README.md` and `master_prompt.md` to reflect paired military time validation, relational string pairing, stateful UI sorting buttons, and root `database/` seed script layout.

---

## [2026-10-30] - Framework Baseline, Blueprint & Quality Assurance Phase

### Added
- **JUnit 5 Unit Test Suite:**
  - Added `ds/UserHashTableTest.java` covering `put()`, `get()`, separate chaining collision handling with low bucket capacities, non-existent key lookups, key value updating, and constructor validation.
  - Added `ds/FlightBSTTest.java` covering flight insertion, in-order traversal (`getSortedFlights()`), chronological departure time ordering, empty tree handling, and duplicate departure times.
  - Added `ds/QuickSortTest.java` covering `sortByPrice()` and `sortByRating()` across empty lists, single-element lists, unsorted multi-element lists, duplicate metrics, and null safety.
  - Added `model/PackageDealTest.java` covering package price summation (`flight.price + hotel.pricePerNight`), hotel rating aggregation, and entity reference validation.
- **Framework & Adaptation Guide (`master_prompt.md`):**
  - Published comprehensive blueprint detailing non-negotiable project rules, decoupled reflection architecture, and domain adaptation mappings (e.g., adapting Travel Booking baseline to Real Estate Management Systems).
- **Open-Source Licensing:**
  - Standardized repository license to the open-source MIT License (`LICENSE`).
- **Production-Grade JavaDoc Documentation:**
  - Added JavaDoc summaries, `@param`, `@return`, `@throws`, and algorithm time complexity annotations (e.g., O(1), O(log n), O(n log n)) across all model, database, data structure, and GUI classes.

### Changed
- **README Documentation Update:**
  - Documented modular package architecture, absolute layout positioning rules (`setLayout(null)`), JDBC connector requirements, database setup schema, and reflection decoupling details.

---

## [2026-10-29] - Core Architecture, Modular Panel Splits & Decoupled DSA Integration

### Added
- **MVC-Inspired Modular Architecture:**
  - Created entity models: `User`, `Flight`, `Hotel`, `PackageDeal`, and `Booking` under `model/`.
  - Created JDBC connection layer `database/DBConnection.java`.
- **Custom Data Structures & Algorithms:**
  - Implemented `ds.UserHashTable` using separate chaining linked lists for user lookup caching.
  - Implemented `ds.FlightBST` binary search tree for in-order flight departure time sorting.
  - Implemented `ds.QuickSort` utility for dual sorting (ascending price / descending hotel rating).
- **Modular Swing Panel Split:**
  - Refactored monolithic GUI interface into distinct modular view panels: `FlightPanel`, `HotelPanel`, `PackagePanel`, and `BookingPanel` hosted inside `MainFrame` tabbed dashboard.
- **Role-Based Access Control (RBAC):**
  - Restricted flight/hotel management controls (Add/Remove forms and actions) exclusively to `ADMIN` accounts.
  - Configured `CUSTOMER` role views for browsing, searching, booking package deals, viewing itineraries, and generating invoice summaries.
- **Decoupled DSA via Java Reflection:**
  - Integrated dynamic class loading (`Class.forName(...)`) in `LoginFrame`, `FlightPanel`, and `PackagePanel` to ensure UI components operate independently of concrete DSA implementations with graceful SQL fallback.
- **GUI Live-Seeding Compliance:**
  - Configured database setup rule retaining only the default pre-seeded `admin` account (`admin` / `admin123`) in database tables.
