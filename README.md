# Nyx - Erebus Load Testing Hub
<p align="center">
    <img src="https://img.shields.io/badge/license-MIT-blue.svg"/>
    <img src="https://img.shields.io/badge/java-21%2B-orange"/>
    <img src="https://img.shields.io/badge/spring%20boot-3.1%2B-brightgreen"/>
</p>

<p align="center">
    <img src="src/main/resources/static/res/logo.png" alt="Nyx logo" height="200"/>
</p>

## The Gateway to Controlled Chaos
> "In Greek mythology, Nyx was the primordial goddess of night - today, she brings darkness to your performance bottlenecks."
> _- Some DevOps poet. Probably._

Nyx serves as the command center for your Erebus load testing ecosystem - a sleek, powerful hub that transforms distributed load generation into a precisely orchestrated performance symphony.

### Key Features

🚀 **Multi-Node Orchestration**  
- Connect unlimited Erebus load-generation nodes  
- Real-time cluster health monitoring  
- Dynamic resource allocation  

🔧 **Project-Centric Workflow**  
- Isolated workspaces for different testing scenarios  
- Dynamic run builder 
- Execution results archive with direct access to individual reports

📊 **Live Telemetry Dashboard**  
- WebSocket-powered real-time metrics  
- Granular node-level statistics  
- Aggregate performance visualizations  

⚡ **Intelligent Execution Control**  
- One-click test initialization 
- Load scenario macro-language support
- Emergency stop functionality  

## Why Nyx?

Traditional load testing tools become unwieldy at scale. Nyx solves this by:

✔ **Reducing cognitive overhead** with intuitive project-based organization  
✔ **Eliminating blind spots** through comprehensive real-time monitoring  
✔ **Maximizing resource utilization** via dynamic node management  

## Quick Start
The recommended way to use this application is a docker-compose deployment:

```bash
# Clone
git clone https://github.com/The-Erebus-Project/Nyx/nyx.git
cd nyx

# Deploy
docker-compose up -d
```
By default, Nyx uses plaintext gRPC connection. If you'd like to enable mTLS - please, refer to [certificates readme](certificates/README.md), set grpc.server.security.enabled flag to true in *application.properties* and un-comment **grpc.server.security** configs.

## Tech Stack
### Core
- Java 21+
- Spring Boot 3
- Socket-IO
### Front-end
- JSP
- Bootstrap 5
- Chart.JS
### Data
- SQLite or JDBC