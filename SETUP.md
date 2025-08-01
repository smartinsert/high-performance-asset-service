# Asset Prototype Setup Guide

This guide will help you set up and run the complete asset prototype system on your Mac.

## Prerequisites

### 1. Java Development Kit (JDK 11+)
```bash
# Check if Java is installed
java -version
javac -version

# If not installed, download from:
# https://adoptium.net/ or use Homebrew:
brew install openjdk@11
```

### 2. Maven
```bash
# Check if Maven is installed
mvn -version

# If not installed:
brew install maven
```

### 3. Redis Installation

#### Install Redis using Homebrew
```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Redis
brew install redis

# Start Redis server
brew services start redis

# Test Redis connection
redis-cli ping
# Should return: PONG
```

#### Alternative Redis Installation (without Homebrew)
```bash
mkdir redis && cd redis
curl -O http://download.redis.io/redis-stable.tar.gz
tar xzvf redis-stable.tar.gz
cd redis-stable
make
make test
sudo make install

# Start Redis
redis-server
```

### 4. VisualVM Installation
```bash
# Download VisualVM from: https://visualvm.github.io/
# Or install via Homebrew:
brew install --cask visualvm
```

## Project Setup

### 1. Create Project Structure
```bash
mkdir high-performance-asset-service
cd high-performance-asset-service

# Create module directories
mkdir -p asset-proto/src/main/proto
mkdir -p asset-service/src/main/java/com/example/service
mkdir -p asset-service/src/main/resources
mkdir -p asset-client/src/main/java/com/example/client  
mkdir -p asset-client/src/main/resources
```

### 2. Copy Source Files
Place all the provided source files in their respective directories:

- `asset-service.proto` → `asset-proto/src/main/proto/`
- All Java files → appropriate `src/main/java/` subdirectories
- Properties files → `src/main/resources/` directories
- POM files → root of each module

### 3. Build the Project
```bash
# Build all modules (run from project root)
mvn clean install

# This will:
# - Generate gRPC stubs from protobuf
# - Compile all Java code
# - Create executable JARs
```

## Running the System

### 1. Start Redis
```bash
# Make sure Redis is running
redis-server
# or if using Homebrew services:
brew services start redis
```

### 2. Start Asset Services

#### Terminal 1: Asset Service 1 (Port 9090)
```bash
cd asset-service
java -Xms512m -Xmx1g -XX:+UseG1GC \
     -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9999 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Dspring.config.location=classpath:service1-application.properties \
     -jar target/asset-service-1.0-SNAPSHOT.jar
```

#### Terminal 2: Asset Service 2 (Port 9091)
```bash
cd asset-service
java -Xms512m -Xmx1g -XX:+UseG1GC \
     -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9998 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Dspring.config.location=classpath:service2-application.properties \
     -jar target/asset-service-1.0-SNAPSHOT.jar
```

#### Terminal 3: Asset Service 3 (Port 9092)
```bash
cd asset-service
java -Xms512m -Xmx1g -XX:+UseG1GC \
     -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9997 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -Dspring.config.location=classpath:service3-application.properties \
     -jar target/asset-service-1.0-SNAPSHOT.jar
```

### 3. Start Asset Client
```bash
# Terminal 4: Asset Client
cd asset-client
java -Xms256m -Xmx512m \
     -Dcom.sun.management.jmxremote \
     -Dcom.sun.management.jmxremote.port=9996 \
     -Dcom.sun.management.jmxremote.authenticate=false \
     -Dcom.sun.management.jmxremote.ssl=false \
     -jar target/asset-client-1.0-SNAPSHOT.jar
```

## VisualVM Profiling

### 1. Start VisualVM
```bash
jvisualvm
# or
visualvm
```

### 2. Connect to Applications
1. In VisualVM, you'll see local Java applications in the left panel
2. Double-click on each "asset-service" and "asset-client" application
3. Navigate to the "Profiler" tab for each application

### 3. Start Profiling
For each application:
1. **CPU Profiling**: Click "CPU" button → Select "Profile classes" → Enter `com.tankit.**`
2. **Memory Profiling**: Click "Memory" button → Click "Start"

### 4. Monitor Performance
- **CPU Usage**: Monitor method execution times and hotspots
- **Memory Usage**: Track heap usage, garbage collection
- **Thread Activity**: Monitor thread pool utilization
- **Cache Statistics**: Available via JMX MBeans

## System Verification

### 1. Check Redis Data
```bash
redis-cli
> SCARD assets:all
# Should return: 100000 (total assets)

> GET asset:ASSET_000001
# Should return JSON representation of asset

> KEYS asset:* | head -10
# Should show asset keys
```

### 2. Test gRPC Services
```bash
# Install grpcurl for testing (optional)
brew install grpcurl

# Test health check
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 com.tankit.asset.AssetService/Check
```

### 3. Monitor Applications
- **Service 1**: http://localhost:8080/actuator/health
- **Service 2**: http://localhost:8081/actuator/health  
- **Service 3**: http://localhost:8082/actuator/health
- **Client**: http://localhost:8090/actuator/health

## Performance Monitoring

### 1. JMX Monitoring
```bash
# Connect JConsole to monitor JMX metrics
jconsole

# Or use jstat for GC monitoring
jstat -gc -t [PID] 5s
```

### 2. Cache Statistics
```bash
# View cache metrics via actuator
curl http://localhost:8080/actuator/metrics/cache.size
curl http://localhost:8080/actuator/metrics/cache.gets
```

### 3. Thread Pool Monitoring
```bash
# Monitor thread pool metrics
curl http://localhost:8080/actuator/metrics/executor.active
curl http://localhost:8080/actuator/metrics/executor.queued
```

## Resource Optimization for Mac

### 1. Memory Settings
For your Mac's 8GB RAM, the recommended JVM settings are:
- **Asset Services**: `-Xms512m -Xmx1g` each
- **Asset Client**: `-Xms256m -Xmx512m`
- **Total JVM Memory**: ~3.5GB (leaving 4.5GB for macOS and other processes)

### 2. GC Optimization
```bash
# Use G1GC for better performance with limited memory
-XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 3. Profiling Optimization
```bash
# Enable flight recorder for low-overhead profiling
-XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=profile.jfr
```

## Troubleshooting

### 1. Redis Connection Issues
```bash
# Check if Redis is running
brew services list | grep redis

# Restart Redis if needed
brew services restart redis
```

### 2. Port Conflicts
```bash
# Check which ports are in use
lsof -i :9090
lsof -i :6379

# Kill processes if needed
kill -9 [PID]
```

### 3. Memory Issues
```bash
# Monitor system memory
top -o MEM

# Reduce JVM heap sizes if needed
# Edit the -Xmx values in startup commands
```

### 4. Build Issues
```bash
# Clean and rebuild
mvn clean install -U

# Skip tests if they fail
mvn clean install -DskipTests
```

## Expected Performance

With this setup, you should observe:
- **Asset Generation**: ~30-60 seconds for 100k assets
- **Cache Population**: ~10-20 seconds for 30k assets per service
- **Client Requests**: ~1-3 seconds for 3000 assets in batches
- **Memory Usage**: ~1-1.5GB per service, ~300-500MB for client
- **Cache Hit Rate**: 40-60% (varies based on request patterns)

The system demonstrates:
- gRPC streaming responses
- Multi-level caching (Caffeine + Redis)
- Thread pool-based request processing
- Inter-service communication
- Performance monitoring with VisualVM