# Flow — Distributed Workflow Automation Engine

A production-grade workflow automation backend similar to n8n and Make.com.
Built with Java Spring Boot, PostgreSQL, Redis, and Gemini AI.

## Live Demo
- **Frontend:** https://flow-frontend-rust.vercel.app
- **API Docs:** https://flow-backend-plq5.onrender.com/swagger-ui.html
- **Health:** https://flow-backend-plq5.onrender.com/api/health

## Architecture
- **Backend:** Java 17 + Spring Boot 3.5 + Spring Security + JWT
- **Database:** PostgreSQL with JPA/Hibernate
- **Queue:** Redis + Redisson (async job processing)
- **AI:** Google Gemini 2.0 Flash API
- **Frontend:** React + React Router + Axios
- **Deploy:** Docker + Render + Vercel

## Key Features
- Webhook and Cron triggers
- Node execution engine with Strategy Pattern
    - HTTP Node — call any external API
    - Condition Node — if/else branching
    - AI Node — Gemini AI processing
    - Notify Node — Slack notifications
- Async Redis queue with exponential backoff retry
- Dead Letter Queue management
- Full execution observability with node-level logs
- JWT authentication
- Rate limiting
- Swagger API documentation

## Tech Stack
Java 17 | Spring Boot 3.5 | PostgreSQL | Redis
JWT | BCrypt | Docker | Render | Vercel | Gemini API
React | React Router | Axios | Nginx

## Local Setup

### Prerequisites
- Docker Desktop
- Java 17
- Node 20

### Run with Docker Compose
```bash
# Clone repos
git clone https://github.com/nivedita6719/flow-backend
git clone https://github.com/nivedita6719/flow-frontend

# Start everything
cd flow-backend
docker compose up
```

App runs at http://localhost:3000

### API Documentation
http://localhost:8080/swagger-ui.html

## Interview Questions
This project demonstrates:
- Distributed systems design
- Async architecture with job queues
- JWT security implementation
- Strategy design pattern
- Production deployment with Docker