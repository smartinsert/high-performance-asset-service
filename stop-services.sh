#!/bin/bash

# Asset Prototype Stop Script
# This script stops all components of the asset prototype system

echo "🛑 Stopping Asset Prototype System..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to stop service by PID
stop_service() {
    local pid=$1
    local service_name=$2
    
    if [ -n "$pid" ] && ps -p $pid > /dev/null 2>&1; then
        echo -e "${YELLOW}Stopping $service_name (PID: $pid)...${NC}"
        kill -TERM $pid
        
        # Wait for graceful shutdown
        local count=0
        while ps -p $pid > /dev/null 2>&1 && [ $count -lt 10 ]; do
            sleep 1
            count=$((count + 1))
        done
        
        # Force kill if still running
        if ps -p $pid > /dev/null 2>&1; then
            echo -e "${RED}Force killing $service_name...${NC}"
            kill -KILL $pid
        fi
        
        echo -e "${GREEN}$service_name stopped${NC}"
    else
        echo -e "${YELLOW}$service_name not running or PID not found${NC}"
    fi
}

# Function to stop services by port
stop_by_port() {
    local port=$1
    local service_name=$2
    
    local pid=$(lsof -ti:$port)
    if [ -n "$pid" ]; then
        echo -e "${YELLOW}Found process on port $port (PID: $pid) - $service_name${NC}"
        stop_service $pid "$service_name"
    else
        echo -e "${YELLOW}No process found on port $port${NC}"
    fi
}

# Check if PID file exists and stop services
if [ -f ".pids" ]; then
    echo "📄 Reading PIDs from .pids file..."
    
    line_num=1
    while IFS= read -r pid; do
        case $line_num in
            1) stop_service $pid "Asset Service 1" ;;
            2) stop_service $pid "Asset Service 2" ;;
            3) stop_service $pid "Asset Service 3" ;;
            4) stop_service $pid "Asset Client" ;;
        esac
        line_num=$((line_num + 1))
    done < ".pids"
    
    # Remove PID file
    rm -f ".pids"
    echo -e "${GREEN}Removed .pids file${NC}"
else
    echo "📄 No .pids file found, stopping by ports..."
    
    # Stop services by their known ports
    stop_by_port 8080 "Asset Service 1"
    stop_by_port 8081 "Asset Service 2"
    stop_by_port 8082 "Asset Service 3"
    stop_by_port 8090 "Asset Client"
    
    # Also check gRPC ports
    stop_by_port 9090 "Asset Service 1 gRPC"
    stop_by_port 9091 "Asset Service 2 gRPC"
    stop_by_port 9092 "Asset Service 3 gRPC"
fi

# Additional cleanup - find any remaining Java processes
echo ""
echo "🔍 Checking for remaining Asset processes..."

asset_processes=$(ps aux | grep -E "(asset-service|asset-client)" | grep -v grep | awk '{print $2}')
if [ -n "$asset_processes" ]; then
    echo -e "${YELLOW}Found remaining Asset processes:${NC}"
    ps aux | grep -E "(asset-service|asset-client)" | grep -v grep
    
    echo -e "${YELLOW}Killing remaining processes...${NC}"
    echo "$asset_processes" | xargs -r kill -TERM
    
    # Wait a moment
    sleep 2
    
    # Force kill if still running
    remaining=$(ps aux | grep -E "(asset-service|asset-client)" | grep -v grep | awk '{print $2}')
    if [ -n "$remaining" ]; then
        echo -e "${RED}Force killing remaining processes...${NC}"
        echo "$remaining" | xargs -r kill -KILL
    fi
fi

# Clean up log files (optional)
if [ "$1" = "--clean-logs" ]; then
    echo ""
    echo "🧹 Cleaning up log files..."
    if [ -d "logs" ]; then
        rm -rf logs/
        echo -e "${GREEN}Removed logs directory${NC}"
    fi
fi

# Check Redis (optional stop)
if [ "$1" = "--stop-redis" ]; then
    echo ""
    echo "🛑 Stopping Redis..."
    if command -v brew &> /dev/null; then
        brew services stop redis
        echo -e "${GREEN}Redis stopped${NC}"
    else
        echo -e "${YELLOW}Homebrew not found, please stop Redis manually${NC}"
    fi
fi

echo ""
echo -e "${GREEN}✅ Asset Prototype System shutdown complete!${NC}"
echo ""
echo "💡 Options:"
echo "  • Use --clean-logs to remove log files"
echo "  • Use --stop-redis to stop Redis service"
echo ""
echo "📊 To verify all processes are stopped:"
echo "  lsof -i :8080,8081,8082,8090,9090,9091,9092"
echo ""