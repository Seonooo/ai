---
description: Setup development environment using Docker Compose
---

# Development Environment Setup

This workflow sets up the necessary infrastructure (MySQL, Redis, Kafka) for the Concert Ticketing Service using Docker Compose.

// turbo
1. Start infrastructure in detached mode
```bash
docker-compose up -d
```

2. (Optional) Check running containers
```bash
docker ps
```
