# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Reference Material Management System (标准物质管理系统) - a full-stack web application for managing reference materials, inventory, procurement, and stock checks in a laboratory environment.

**Tech Stack:**
- Frontend: Vue 3 + Vite + Element Plus + Pinia + Vue Router 5
- Backend: Spring Boot 3.2 + MyBatis-Plus + Spring Security + JWT
- Database: MySQL
- API Documentation: Knife4j (OpenAPI 3)

## Commands

### Development

```bash
# Start all services (backend + frontend) - recommended for development
./dev.sh                    # Foreground mode, Ctrl+C to stop
./scripts/start-all.sh start    # Background mode with status checks
./scripts/start-all.sh stop     # Stop all services
./scripts/start-all.sh status   # Check service status
./scripts/start-all.sh logs     # View logs

# Frontend only
cd frontend && npm run dev -- --port 3002

# Backend only
cd backend && mvn spring-boot:run
```

**Service URLs:**
- Frontend: http://localhost:3002
- Backend: http://localhost:8080
- API Docs: http://localhost:8080/doc.html
- Default credentials: admin / admin123

### Build

```bash
# Frontend
cd frontend && npm run build

# Backend
cd backend && mvn clean package -DskipTests
```

### Testing

```bash
# E2E tests (requires services running)
./scripts/run-tests.sh                  # Run all Playwright tests
./scripts/run-tests.sh test-login.js    # Run single test

# API health check
./scripts/e2e-test.sh
```

### Database

**IMPORTANT: MySQL runs in local Docker container - DO NOT attempt to install/deploy MySQL locally.**

```bash
# MySQL Docker container
docker container name: mysql-dev

# Connect to MySQL in container (interactive)
docker exec -it mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4

# Initialize database (MUST use --default-character-set=utf8mb4 to avoid garbled Chinese)
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 < database/init.sql
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/init-data.sql
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/phase2-tables.sql
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/phase3-tables.sql
docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < database/phase4-tables.sql

# 数据库cli
macos环境: docker exec -i mysql-dev mysql -u root -p123456 --default-character-set=utf8mb4 reference_material_management < scripts/init-test-data.sql
WINDOW环境: c/Program Files/MySQL/MySQL Shell 8.0/bin/mysqlsh.exe
# Check container status
docker ps | grep mysql-dev
```

**Database config:** `backend/src/main/resources/application.yml`
- Host: localhost (Docker port mapped)
- Database: `reference_material_management`
- Credentials: root / 123456
- ⚠️ Always use `--default-character-set=utf8mb4` to prevent Chinese character encoding issues

## Architecture

### Frontend Structure

```
frontend/src/
├── api/           # API request modules (axios-based, one per domain)
├── views/         # Page components (route-based organization)
├── components/    # Reusable components
├── router/        # Vue Router configuration with auth guards
├── store/         # Pinia stores (modules/user.js)
├── utils/         # Utilities (auth.js for token management)
└── styles/        # Global styles and themes
```

**Key patterns:**
- Token stored in localStorage via `@/utils/auth.js`
- Route guard checks token and fetches user info if missing
- API modules follow consistent pattern with axios

### Backend Structure

```
backend/src/main/java/com/rmm/
├── controller/    # REST API endpoints
├── service/       # Business logic layer
├── mapper/        # MyBatis-Plus mappers
├── entity/        # Database entities
├── dto/           # Data Transfer Objects (input)
├── vo/            # View Objects (output)
├── config/        # Spring configurations (Security, CORS, MyBatis-Plus)
├── filter/        # JWT authentication filter
├── exception/     # Exception handling
├── common/        # Common utilities
└── util/          # Utilities (JwtUtil)
```

**API pattern:**
- Controllers handle HTTP, delegate to services
- Services use MyBatis-Plus mappers
- DTOs for request bodies, VOs for responses
- JWT authentication via filter chain

### Database Schema

**Core tables:**
- `user` / `role` - User management with role-based access
- `category` - Tree structure for material classification
- `location` - Storage locations
- `reference_material` - Master data for reference materials
- `stock` - Current inventory
- `stock_in` / `stock_out` - Inbound/outbound records
- `purchase` - Procurement management
- `stock_check` / `stock_check_item` - Inventory count
- `alert_config` / `alert_record` - Expiry alerts
- `supplier` - Supplier information
- `metadata` - Configurable dropdowns (reasons, conditions)

**Key relationships:**
- `stock.material_id` → `reference_material.id`
- `stock.category_id` → `category.id` (via material)
- `stock.location_id` → `location.id`
- `stock_out.stock_id` → `stock.id` (links outbound to inventory)

## Module Breakdown

| Module | Frontend View | Backend Controller | Purpose |
|--------|---------------|-------------------|---------|
| Dashboard | `views/dashboard/` | `DashboardController` | Statistics overview |
| Basic Data | `views/basic/` | CategoryController, LocationController, MetadataController | Categories, locations, metadata |
| Materials | `views/reference-material/` | `ReferenceMaterialController` | Reference material master data |
| Stock | `views/stock/` | `StockController` | Inventory list |
| Stock-In | `views/stock-in/` | `StockInController` | Inbound records |
| Stock-Out | `views/stock-out/` | `StockOutController` | Outbound with approval workflow |
| Purchase | `views/purchase/` | `PurchaseController` | Procurement requests |
| Stock Check | `views/stock-check/` | `StockCheckController` | Inventory counts |
| Alerts | `views/alert/` | `AlertController` | Expiry warnings |
| System | `views/system/` | UserController, RoleController | User/role management |

## E2E Testing

Playwright-based tests in `scripts/e2e-tests/`. Each test file corresponds to a module:
- `test-login.js` - Authentication
- `test-dashboard.js` - Dashboard statistics
- `test-category.js`, `test-location.js` - Basic data
- `test-stock.js`, `test-stock-in.js`, `test-stock-out.js` - Inventory
- `test-purchase.js`, `test-stock-check.js` - Procurement/counting
- `test-alert.js` - Alert management
- `test-user.js` - User management

Tests use shared utilities from `common.js`.
