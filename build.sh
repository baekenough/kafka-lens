#!/bin/bash
#
# kafka-lens - Single JAR Build Script
# Builds frontend (Next.js static export) and packages into backend JAR
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Project directories
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/kafka-lens-frontend"
BACKEND_DIR="$PROJECT_ROOT/kafka-lens-backend"
STATIC_DIR="$BACKEND_DIR/src/main/resources/static"

# Functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_requirements() {
    log_info "Checking requirements..."

    # Check Node.js
    if ! command -v node &> /dev/null; then
        log_error "Node.js is not installed. Please install Node.js 18+"
        exit 1
    fi

    # Check npm
    if ! command -v npm &> /dev/null; then
        log_error "npm is not installed."
        exit 1
    fi

    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed. Please install Java 21+"
        exit 1
    fi

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_error "Maven is not installed. Please install Maven 3.9+"
        exit 1
    fi

    log_info "All requirements satisfied."
}

build_frontend() {
    log_info "Building frontend (Next.js)..."

    cd "$FRONTEND_DIR"

    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        log_info "Installing npm dependencies..."
        npm install
    fi

    # Build (static export)
    npm run build

    # Verify build output
    if [ ! -d "out" ]; then
        log_error "Frontend build failed: 'out' directory not found"
        exit 1
    fi

    log_info "Frontend build completed."
}

copy_static_files() {
    log_info "Copying frontend build to backend static resources..."

    # Create static directory if not exists
    mkdir -p "$STATIC_DIR"

    # Clean existing static files (except application files)
    if [ -d "$STATIC_DIR" ]; then
        rm -rf "$STATIC_DIR"/*
    fi

    # Copy frontend build output
    cp -r "$FRONTEND_DIR/out/"* "$STATIC_DIR/"

    # Verify copy
    if [ ! -f "$STATIC_DIR/index.html" ]; then
        log_error "Static file copy failed: index.html not found"
        exit 1
    fi

    log_info "Static files copied successfully."
}

build_backend() {
    log_info "Building backend (Spring Boot)..."

    cd "$BACKEND_DIR"

    # Maven build
    mvn clean package -DskipTests

    # Find JAR file
    JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" | head -1)

    if [ -z "$JAR_FILE" ]; then
        log_error "Backend build failed: JAR file not found"
        exit 1
    fi

    log_info "Backend build completed: $JAR_FILE"
}

print_summary() {
    echo ""
    echo "=========================================="
    echo -e "${GREEN}Build completed successfully!${NC}"
    echo "=========================================="
    echo ""
    echo "JAR file location:"
    echo "  $BACKEND_DIR/target/kafka-lens-backend-0.0.1-SNAPSHOT.jar"
    echo ""
    echo "Run the application:"
    echo "  java -jar $BACKEND_DIR/target/kafka-lens-backend-0.0.1-SNAPSHOT.jar"
    echo ""
    echo "Access the application:"
    echo "  http://localhost:8080"
    echo ""
}

# Main
main() {
    echo "=========================================="
    echo "  kafka-lens - Single JAR Build"
    echo "=========================================="
    echo ""

    check_requirements
    build_frontend
    copy_static_files
    build_backend
    print_summary
}

# Run main
main "$@"
