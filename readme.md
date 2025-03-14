# Netty Proxy Application

## 🌟 Project Overview
This is a lightweight proxy server built with Netty and Spring Boot that provides:
- **Request Routing** based on configuration rules
- **Dynamic Configuration Management** via API
- **CSV-based Routing Configuration**

### How the Proxy Works
1. **Bootstraps** with routing rules from `config.csv` (or specified path)
2. **Listens** for incoming client requests
3. **Matches** requests against routing rules
4. **Forwards** matched requests to target servers
5. **Returns** responses through the proxy channel

## 🚀 Quick Start
bash mvn spring-boot:run

## ⚙️ Configuration

### 1. File-based Initialization
properties
application.properties
router.config.path=./config.csv# Default path if not specified
api.port=8081 # Default API port


### 2. API Configuration Management
Access Swagger UI at:  
`http://localhost:${api.port}/swagger-ui.html` (default: *8081*)


## 🔧 Key Components

### Application Configuration (ApplicationConfig.java)
java @Value("${router.config.path:./config.csv}") // Default config path private String filePath;
@Value("${api.port:8081}") // Default API port private Integer apiPort;

> 💡 **Tips**:
> - Use `-Drouter.config.path=/custom/path.csv` to override config location
> - API port can be changed via `-Dapi.port=8081` at startup
> - Configuration changes via API persist only in memory (consider implementing persistence layer)


