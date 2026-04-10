#!/usr/bin/env bash

# SynapseCore guided explainer
# ----------------------------
# This script is intentionally verbose. Its job is not only to run commands,
# but to act like a readable onboarding tour for engineers and future agents.
#
# Use it when you want a plain-language explanation of:
# - what SynapseCore is
# - what the MVP proves
# - how the folders fit together
# - how the backend is organized
# - how data moves through the system
# - how to get productive in the repo quickly
#
# Run with:
#   bash scripts/explain-project.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

section() {
  local title="$1"
  echo
  echo "=================================================="
  echo "$title"
  echo "=================================================="
}

note() {
  echo "$1"
}

section "SYNAPSECORE OVERVIEW"
note "Project root: $ROOT_DIR"
echo
note "SynapseCore is a real-time operational intelligence platform."
note "It connects to business systems and turns operational activity into:"
note "- live understanding"
note "- predictions"
note "- alerts"
note "- recommendations"
echo
note "The product behaves like a live operational brain for a company."
note "It does not replace company systems."
note "It sits above them as an intelligence layer that watches operations, updates"
note "its view of reality, estimates what is likely to happen next, and pushes"
note "the right operational signal back to humans in real time."

section "WHAT PROBLEM THE PROJECT SOLVES"
note "Many companies already have order systems, inventory systems, warehouse tools,"
note "status spreadsheets, and reporting dashboards."
echo
note "The common problem is not a total lack of data. The real problem is that data"
note "is fragmented, delayed, and hard to interpret in time."
echo
note "That causes operational pain such as:"
note "- slow decisions"
note "- late issue detection"
note "- stockouts"
note "- poor coordination"
note "- manual fire-fighting"
note "- revenue loss from reacting too slowly"
echo
note "SynapseCore exists to close that gap."
note "Its value is not just showing the business what happened."
note "Its value is helping the business understand what changed, what risk is"
note "emerging, and what action should happen next."

section "WHAT THE MVP PROVES"
note "The MVP is intentionally sharp."
note "It proves one operational loop instead of pretending to solve everything."
echo
note "The MVP proof flow is:"
note "order comes in"
note "-> inventory updates"
note "-> low stock is detected"
note "-> alert is generated"
note "-> recommendation is generated"
note "-> dashboard updates live"
echo
note "If this loop works end to end, SynapseCore is no longer a static app shell."
note "It becomes a believable operational intelligence system."

section "HOW THE SYSTEM BEHAVES"
note "SynapseCore should be understood as a continuous system loop:"
echo
note "1. receive new business activity"
note "2. update internal operational state"
note "3. analyze what changed"
note "4. detect meaningful risk"
note "5. estimate likely future outcomes"
note "6. generate alerts"
note "7. generate recommendations"
note "8. push live updates to the dashboard"
echo
note "That loop is the identity of the product."
note "If the code or UI stops reflecting this event-driven behavior, the product"
note "has drifted away from its purpose."

section "TOP-LEVEL REPOSITORY TOUR"
note "/backend"
note "The Java Spring Boot backend and the core engine of SynapseCore."
note "This is where ingestion, persistence, intelligence, prediction, decision,"
note "alerts, recommendations, realtime publishing, and simulation are coordinated."
echo
note "/frontend"
note "The React + Vite dashboard."
note "This is the live operational surface where users see summary metrics, alerts,"
note "recommendations, inventory posture, recent orders, and simulation controls."
echo
note "/infrastructure"
note "Docker, docker-compose, environment files, and local infrastructure support."
note "This folder helps developers run PostgreSQL, Redis, the backend, and the"
note "frontend together in a repeatable way."
echo
note "/docs"
note "Architecture explanations, API documentation, system flow notes, and technical"
note "understanding for people onboarding into the project."
echo
note "/scripts"
note "Helper scripts for setup, startup, seeding, explanation, and developer onboarding."
note "These scripts are meant to reduce friction and help people understand the repo"
note "quickly, not just automate a few commands."

section "BACKEND PACKAGE TOUR"
note "backend/src/main/java/com/synapsecore/config"
note "Configuration classes such as WebSocket setup, Redis setup, CORS rules,"
note "scheduling support, and application-level infrastructure settings."
echo
note "backend/src/main/java/com/synapsecore/integration"
note "The place where external systems connect into SynapseCore."
note "This is the natural home for ingestion endpoints, connectors, webhooks,"
note "and future adapter logic."
echo
note "backend/src/main/java/com/synapsecore/event"
note "Internal business event recording and event-query logic."
note "This package helps make system activity inspectable and keeps the operational"
note "story of the platform visible."
echo
note "backend/src/main/java/com/synapsecore/domain"
note "The core business model of the system."
note "This is where the foundational operational state lives."
echo
note "backend/src/main/java/com/synapsecore/domain/entity"
note "Database-backed business entities such as Product, Warehouse, Inventory,"
note "CustomerOrder, OrderItem, Alert, Recommendation, and BusinessEvent."
echo
note "backend/src/main/java/com/synapsecore/domain/repository"
note "Persistence interfaces for reading and writing the domain model through JPA."
echo
note "backend/src/main/java/com/synapsecore/domain/service"
note "Core business services that implement system behavior."
note "This is where order ingestion, inventory update flows, dashboard summary logic,"
note "data seeding, and operational views are coordinated."
echo
note "backend/src/main/java/com/synapsecore/domain/dto"
note "Request and response objects used to keep API contracts clean, explicit,"
note "and safe for frontend or external callers."
echo
note "backend/src/main/java/com/synapsecore/intelligence"
note "Business logic that detects meaningful operational conditions."
note "For the MVP, this includes low-stock detection and consumption-aware inventory"
note "risk interpretation."
echo
note "backend/src/main/java/com/synapsecore/prediction"
note "Logic that estimates future outcomes like stock depletion time."
note "This is explainable rule-based prediction, not machine learning."
echo
note "backend/src/main/java/com/synapsecore/decision"
note "Logic that converts system understanding into recommended actions."
note "Examples include reorder decisions and urgency escalation."
echo
note "backend/src/main/java/com/synapsecore/access"
note "Control-role enforcement for protected operational actions."
note "This package makes sure approvals, connector changes, and replay actions"
note "are allowed only for actors with the correct operational authority."
echo
note "backend/src/main/java/com/synapsecore/auth"
note "Signed-in user session handling mapped onto SynapseCore operators."
note "This is where lightweight identity, session state, and demo-ready local"
note "credential handling live today."
echo
note "backend/src/main/java/com/synapsecore/alert"
note "Creation and management of alerts so operational issues are surfaced in a"
note "structured, decision-friendly way."
echo
note "backend/src/main/java/com/synapsecore/realtime"
note "WebSocket and live update broadcasting logic."
note "This is what makes the dashboard feel alive instead of refresh-driven."
echo
note "backend/src/main/java/com/synapsecore/simulation"
note "Fake data generation and simulation state handling so the MVP can demonstrate"
note "real behavior before enterprise integrations are connected."
echo
note "backend/src/main/java/com/synapsecore/api/controller"
note "REST endpoints used by the frontend and by external systems interacting with"
note "the SynapseCore engine."

section "IMPORTANT FILES TO UNDERSTAND FIRST"
note "README.md"
note "Main entry point for understanding the repo, the MVP, how to run it, and how"
note "to demo the operational flow."
echo
note "AGENTS.md"
note "Repository guidance for Codex and future agents."
note "It explains product identity, MVP guardrails, architecture rules, and commands"
note "that should remain aligned with the project."
echo
note "infrastructure/docker-compose.yml"
note "Defines the local stack and supporting services, including PostgreSQL, Redis,"
note "and the application containers when the environment is healthy."
echo
note "backend/src/main/resources/application.yml"
note "Shared backend runtime configuration for Spring Boot, session behavior,"
note "management endpoints, and SynapseCore feature settings."
echo
note "backend/src/main/resources/application-dev.yml and application-prod.yml"
note "Profile-specific backend runtime behavior for local development versus"
note "deployment-oriented operation."
echo
note "backend/pom.xml"
note "Backend dependency and build configuration for the Spring Boot application."
echo
note "frontend/package.json"
note "Frontend dependency and script configuration for the React + Vite dashboard."
echo
note "backend/Dockerfile and frontend/Dockerfile"
note "Container build definitions for the application services."

section "HOW DATA FLOWS THROUGH THE SYSTEM"
note "The fastest way to understand SynapseCore is to follow one order."
echo
note "1. A client sends POST /api/orders with a warehouse and line items."
note "2. The backend validates the request DTOs."
note "3. OrderService creates the CustomerOrder and OrderItem records."
note "4. Matching Inventory records are located and quantityAvailable is reduced."
note "5. BusinessEvent records are written so the operational story is traceable."
note "6. InventoryMonitoringService evaluates the changed inventory state."
note "7. Prediction logic estimates depletion risk."
note "8. Intelligence logic decides whether the item is low stock or at elevated risk."
note "9. RecommendationService creates an action recommendation when needed."
note "10. AlertService creates or updates the operational alert."
note "11. DashboardService refreshes the top-level metrics."
note "12. RealtimeService publishes the changed state over WebSockets."
note "13. The frontend updates the control-center view without a manual refresh."
echo
note "Simulation follows the same core path wherever possible."
note "That matters because SynapseCore should feel alive through real business logic,"
note "not through a fake parallel demo mode."

section "HOW TO RUN THE PROJECT"
note "Full stack with Docker:"
note "  cd infrastructure"
note "  docker compose up --build"
echo
note "Local development flow:"
note "  1. cd infrastructure && docker compose up -d postgres redis"
note "  2. cd backend && ./mvnw spring-boot:run"
note "  3. cd frontend && npm install && npm run dev"
echo
note "Important onboarding scripts:"
note "  bash scripts/explain-project.sh"
note "  bash scripts/project-tree.sh"
note "  bash scripts/full-structure.sh"
echo
note "For a clean demo baseline after services are up, run:"
note "  bash scripts/seed.sh"
echo
note "If you want the baseline reset and simulation started together, run:"
note "  bash scripts/seed.sh --with-simulation"

section "HOW TO UNDERSTAND THE ARCHITECTURE QUICKLY"
note "If you are new to the repo, use this sequence:"
echo
note "1. Read README.md for the product and run flow."
note "2. Run bash scripts/explain-project.sh for a guided summary."
note "3. Run bash scripts/project-tree.sh for the important structure."
note "4. Read docs/architecture.md for package and module boundaries."
note "5. Read docs/system-flow.md for the operational event loop."
note "6. Read docs/deployment.md for profile, env, and deployment configuration."
note "7. Inspect OrderService, InventoryMonitoringService, AlertService,"
note "   RecommendationService, RealtimeService, and App.jsx."
echo
note "Those files show the main business path from incoming activity to live decision output."

section "FINAL ORIENTATION"
note "SynapseCore is not just storing orders and inventory."
note "It is trying to turn operational change into real-time decisions."
echo
note "Protect these principles as the system grows:"
note "- event-driven behavior over static reporting"
note "- explicit intelligence and decision logic over vague magic"
note "- realtime visibility over manual refresh loops"
note "- clear modular boundaries over random helper sprawl"
note "- practical MVP depth over generic feature breadth"
echo
note "Guided explanation complete."
