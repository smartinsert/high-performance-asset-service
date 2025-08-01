#!/bin/bash

# Asset Prototype Startup Script
# This script starts all components of the asset prototype system

echo "🚀 Starting Asset Prototype System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if a port is in use
check_port() {
    if lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null ; then
        echo -e "${RED}Port $1 is already in use!${NC}"
        return 1
    else
        return 0
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local port=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${YELLOW}Waiting for $service_name to start on port $port...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:$port/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}$service_name is ready!${NC}"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}$service_name failed to start within $((max_attempts * 2)) seconds${NC}"
    return 1
}

# Check prerequisites
echo "🔍 Checking prerequisites..."

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Java is not installed. Please install OpenJDK 11+${NC}"
    exit 1
fi

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven is not installed. Please install Maven${NC}"
    exit 1
fi

echo "🔍 Checking port availability for Redis"
# Check Redis
if ! command -v redis-cli &> /dev/null; then
    echo -e "${RED}Redis is not installed. Please install Redis${NC}"
    exit 1
fi

check_port 6379 || { echo "Redis port"; exit 1; }

# Check if Redis is running
if ! redis-cli ping > /dev/null 2>&1; then
    echo -e "${YELLOW}Starting Redis...${NC}"
    brew services start redis
    sleep 3
    
    if ! redis-cli ping > /dev/null 2>&1; then
        echo -e "${RED}Failed to start Redis. Please start Redis manually${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ All prerequisites satisfied${NC}"

# Check if ports are available
echo "🔍 Checking port availability..."
check_port 8080 || { echo "Service 1 HTTP port"; exit 1; }
check_port 8081 || { echo "Service 2 HTTP port"; exit 1; }
check_port 8082 || { echo "Service 3 HTTP port"; exit 1; }
check_port 8090 || { echo "Client HTTP port"; exit 1; }
check_port 9090 || { echo "Service 1 gRPC port"; exit 1; }
check_port 9091 || { echo "Service 2 gRPC port"; exit 1; }
check_port 9092 || { echo "Service 3 gRPC port"; exit 1; }

echo -e "${GREEN}✅ All ports are available${NC}"

# Build project
echo "🔨 Building project..."
if ! mvn clean install -q; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Build completed successfully${NC}"

# JVM settings for Mac with 8GB RAM
JVM_OPTS_SERVICE="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JVM_OPTS_CLIENT="-Xms256m -Xmx512m -XX:+UseG1GC"
JMX_OPTS_BASE="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

# Create logs directory
mkdir -p logs

# Start services in background
echo "🚀 Starting Asset Services..."

# Asset Service 1 (Primary - initializes data)
echo -e "${BLUE}Starting Asset Service 1 (Port 9090)...${NC}"
java $JVM_OPTS_SERVICE $JMX_OPTS_BASE \
    -Dcom.sun.management.jmxremote.port=9999 \
    -Dspring.config.location=classpath:service1-application.properties \
    -jar asset-service/target/asset-service-1.0-SNAPSHOT.jar \
    > logs/service1.log 2>&1 &
SERVICE1_PID=$!
echo "Asset Service 1 PID: $SERVICE1_PID"

# Wait for service 1 to initialize data
sleep 10
wait_for_service 8080 "Asset Service 1"

# Asset Service 2
echo -e "${BLUE}Starting Asset Service 2 (Port 9091)...${NC}"
java $JVM_OPTS_SERVICE $JMX_OPTS_BASE \
    -Dcom.sun.management.jmxremote.port=9998 \
    -Dspring.config.location=classpath:service2-application.properties \
    -jar asset-service/target/asset-service-1.0-SNAPSHOT.jar \
    > logs/service2.log 2>&1 &
SERVICE2_PID=$!
echo "Asset Service 2 PID: $SERVICE2_PID"

# Asset Service 3
echo -e "${BLUE}Starting Asset Service 3 (Port 9092)...${NC}"
java $JVM_OPTS_SERVICE $JMX_OPTS_BASE \
    -Dcom.sun.management.jmxremote.port=9997 \
    -Dspring.config.location=classpath:service3-application.properties \
    -jar asset-service/target/asset-service-1.0-SNAPSHOT.jar \
    > logs/service3.log 2>&1 &
SERVICE3_PID=$!
echo "Asset Service 3 PID: $SERVICE3_PID"

# Wait for all services to be ready
wait_for_service 8081 "Asset Service 2"
wait_for_service 8082 "Asset Service 3"

echo -e "${GREEN}✅ All Asset Services are running${NC}"

# Start client
echo -e "${BLUE}Starting Asset Client...${NC}"
java $JVM_OPTS_CLIENT $JMX_OPTS_BASE \
    -Dcom.sun.management.jmxremote.port=9996 \
    -jar asset-client/target/asset-client-1.0-SNAPSHOT.jar \
    > logs/client.log 2>&1 &
CLIENT_PID=$!
echo "Asset Client PID: $CLIENT_PID"

wait_for_service 8090 "Asset Client"

echo ""
echo -e "${GREEN}🎉 Asset Prototype System is now running!${NC}"
echo ""
echo "📊 Service Endpoints:"
echo "  • Asset Service 1: http://localhost:8080/actuator/health (gRPC: 9090)"
echo "  • Asset Service 2: http://localhost:8081/actuator/health (gRPC: 9091)"  
echo "  • Asset Service 3: http://localhost:8082/actuator/health (gRPC: 9092)"
echo "  • Asset Client:    http://localhost:8090/actuator/health"
echo ""
echo "📈 JMX Monitoring Ports:"
echo "  • Asset Service 1: localhost:9999"
echo "  • Asset Service 2: localhost:9998"
echo "  • Asset Service 3: localhost:9997"
echo "  • Asset Client:    localhost:9996"
echo ""
echo "📄 Log Files:"
echo "  • Service 1: logs/service1.log"
echo "  • Service 2: logs/service2.log"
echo "  • Service 3: logs/service3.log"
echo "  • Client:    logs/client.log"
echo ""
echo "🔧 To monitor with VisualVM:"
echo "  1. Run: jvisualvm"
echo "  2. Connect to local applications"
echo "  3. Enable CPU and Memory profiling"
echo ""
echo "🛑 To stop all services:"
echo "  ./stop-services.sh"
echo ""

# Save PIDs for cleanup
echo "$SERVICE1_PID" > .pids
echo "$SERVICE2_PID" >> .pids
echo "$SERVICE3_PID" >> .pids
echo "$CLIENT_PID" >> .pids

echo -e "${YELLOW}System startup complete. Press Ctrl+C to view logs or run ./stop-services.sh to shutdown.${NC}"

# Optional: tail logs
if [ "$1" = "--logs" ]; then
    echo ""
    echo "📄 Tailing all logs..."
    tail -f logs/*.log
fi