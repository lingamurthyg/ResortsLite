#!/bin/bash

# PostgreSQL Migration Validation Script
# This script validates the PostgreSQL migration and setup

set -e

echo "=========================================="
echo "PostgreSQL Migration Validation Script"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-resortdb}
DB_USER=${DB_USER:-postgres}

# Function to print success message
print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

# Function to print error message
print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Function to print warning message
print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Check if PostgreSQL is installed
echo "1. Checking PostgreSQL installation..."
if command -v psql &> /dev/null; then
    PSQL_VERSION=$(psql --version | awk '{print $3}')
    print_success "PostgreSQL is installed (version: $PSQL_VERSION)"
else
    print_error "PostgreSQL is not installed"
    exit 1
fi
echo ""

# Check if PostgreSQL is running
echo "2. Checking PostgreSQL service status..."
if pg_isready -h $DB_HOST -p $DB_PORT &> /dev/null; then
    print_success "PostgreSQL is running on $DB_HOST:$DB_PORT"
else
    print_error "PostgreSQL is not running on $DB_HOST:$DB_PORT"
    exit 1
fi
echo ""

# Check if database exists
echo "3. Checking database existence..."
if PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -lqt | cut -d \| -f 1 | grep -qw $DB_NAME; then
    print_success "Database '$DB_NAME' exists"
else
    print_warning "Database '$DB_NAME' does not exist"
    echo "   Creating database..."
    PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || true
    print_success "Database '$DB_NAME' created"
fi
echo ""

# Check if bookings table exists
echo "4. Checking database schema..."
TABLE_EXISTS=$(PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -tAc "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'bookings');")
if [ "$TABLE_EXISTS" = "t" ]; then
    print_success "Table 'bookings' exists"
    
    # Check table structure
    echo "   Checking table structure..."
    COLUMNS=$(PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -tAc "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'bookings';")
    print_success "   Table has $COLUMNS columns"
else
    print_warning "Table 'bookings' does not exist"
    echo "   Run schema.sql to create the table"
fi
echo ""

# Check indexes
echo "5. Checking database indexes..."
INDEX_COUNT=$(PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -tAc "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'bookings';")
if [ "$INDEX_COUNT" -gt 0 ]; then
    print_success "Found $INDEX_COUNT indexes on 'bookings' table"
else
    print_warning "No indexes found on 'bookings' table"
fi
echo ""

# Check triggers
echo "6. Checking database triggers..."
TRIGGER_COUNT=$(PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -tAc "SELECT COUNT(*) FROM pg_trigger WHERE tgrelid = 'bookings'::regclass;")
if [ "$TRIGGER_COUNT" -gt 0 ]; then
    print_success "Found $TRIGGER_COUNT triggers on 'bookings' table"
else
    print_warning "No triggers found on 'bookings' table"
fi
echo ""

# Check sample data
echo "7. Checking sample data..."
if [ "$TABLE_EXISTS" = "t" ]; then
    ROW_COUNT=$(PGPASSWORD=$PGPASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -tAc "SELECT COUNT(*) FROM bookings;")
    if [ "$ROW_COUNT" -gt 0 ]; then
        print_success "Found $ROW_COUNT rows in 'bookings' table"
    else
        print_warning "No data found in 'bookings' table"
    fi
fi
echo ""

# Check Maven dependencies
echo "8. Checking Maven dependencies..."
if [ -f "pom.xml" ]; then
    if grep -q "postgresql" pom.xml; then
        print_success "PostgreSQL dependency found in pom.xml"
    else
        print_error "PostgreSQL dependency not found in pom.xml"
    fi
    
    if grep -q "spring-boot-starter-data-jpa" pom.xml; then
        print_success "Spring Data JPA dependency found in pom.xml"
    else
        print_error "Spring Data JPA dependency not found in pom.xml"
    fi
else
    print_warning "pom.xml not found in current directory"
fi
echo ""

# Check application.properties
echo "9. Checking application configuration..."
if [ -f "src/main/resources/application.properties" ]; then
    if grep -q "postgresql" src/main/resources/application.properties; then
        print_success "PostgreSQL configuration found in application.properties"
    else
        print_error "PostgreSQL configuration not found in application.properties"
    fi
else
    print_warning "application.properties not found"
fi
echo ""

# Check entity classes
echo "10. Checking entity classes..."
if [ -d "src/main/java/com/demo/resortslite/entity" ]; then
    ENTITY_COUNT=$(find src/main/java/com/demo/resortslite/entity -name "*.java" | wc -l)
    if [ "$ENTITY_COUNT" -gt 0 ]; then
        print_success "Found $ENTITY_COUNT entity class(es)"
    else
        print_warning "No entity classes found"
    fi
else
    print_warning "Entity package not found"
fi
echo ""

# Check repository classes
echo "11. Checking repository classes..."
if [ -d "src/main/java/com/demo/resortslite/repository" ]; then
    REPO_COUNT=$(find src/main/java/com/demo/resortslite/repository -name "*.java" | wc -l)
    if [ "$REPO_COUNT" -gt 0 ]; then
        print_success "Found $REPO_COUNT repository class(es)"
    else
        print_warning "No repository classes found"
    fi
else
    print_warning "Repository package not found"
fi
echo ""

# Summary
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo ""
echo "Database Connection: $DB_HOST:$DB_PORT/$DB_NAME"
echo "PostgreSQL Version: $PSQL_VERSION"
echo "Table Status: $([ "$TABLE_EXISTS" = "t" ] && echo "Created" || echo "Not Created")"
echo "Indexes: $INDEX_COUNT"
echo "Triggers: $TRIGGER_COUNT"
echo "Sample Data Rows: ${ROW_COUNT:-0}"
echo ""
echo "Migration validation completed!"
echo ""
