#!/usr/bin/env bash
cat <<'EOF'
CODEX BRIEF FOR SYNAPSECORE

You are the lead developer for SynapseCore.

Use the existing folder structure as the foundation.

Build:
1. An operational brain in backend/
2. A live control center in frontend/
3. PostgreSQL + Redis + WebSocket wiring
4. The MVP loop:
   order -> inventory -> low stock -> alert -> recommendation -> realtime update
5. Simulation through the same real services

Rules:
- Keep architecture modular
- Do not collapse everything into one package
- Preserve the event-driven structure
- Start with MVP scope only
- Do not add Kafka in the MVP
- Do not turn it into a generic admin dashboard
- Add Docker-friendly configuration
EOF
