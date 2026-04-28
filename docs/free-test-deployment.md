# Free Test Deployment Guide

This guide is the fastest zero-domain-cost path to get SynapseCore publicly reachable for real browser testing.

## Best Fit

Use this guide when you want:

- a public test deployment
- no paid domain purchase yet
- a low-friction way to validate the full HTTPS app and API flow

The intended shape is:

- Oracle Cloud Always Free Ubuntu VM
- public IP on that VM
- `sslip.io` or `nip.io` hostnames generated from the public IP
- SynapseCore public compose with built-in Caddy HTTPS

## Important Boundaries

This is a **testing / staging path**, not the final recommended home for real company production.

Why:

- free-tier capacity can be limited
- free resources are not the same as a paid production SLA
- public testing and company production should not be treated as the same risk level

## 1. Create The Free VM

Recommended target:

- Oracle Cloud Always Free
- Ubuntu `24.04`
- public IPv4 address attached

Recommended minimum host shape for SynapseCore testing:

- `1 OCPU / 6 GB RAM` or `2 OCPU / 12 GB RAM`

Make sure these inbound ports are allowed:

- `22` for SSH
- `80` for HTTP
- `443` for HTTPS

## 2. Copy The Repo To The VM

On the VM:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker "$USER"
newgrp docker

mkdir -p ~/apps
cd ~/apps
git clone YOUR_REPOSITORY_URL synapsecore
cd synapsecore
```

## 3. Generate Free Public Test Env Files

Use the helper script with the VM's public IP and your ACME email.

Example:

```bash
bash scripts/prepare-free-test-envs.sh --public-ip 203.0.113.10 --email you@example.com
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-free-test-envs.ps1 -PublicIp 203.0.113.10 -AcmeEmail you@example.com
```

This fills:

- `infrastructure/env/backend.prod.env`
- `infrastructure/env/frontend.prod.env`
- `infrastructure/env/edge.prod.env`

And generates public test hostnames like:

- `https://app.203.0.113.10.sslip.io`
- `https://api.203.0.113.10.sslip.io`

If you prefer, you can swap to `nip.io`:

```bash
bash scripts/prepare-free-test-envs.sh --public-ip 203.0.113.10 --email you@example.com --dns-suffix nip.io
```

## 4. Gate The Release

Run:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env bash scripts/release-readiness.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\release-readiness.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env
```

## 5. Start The Public Stack

Run:

```bash
BACKEND_ENV_FILE=./env/backend.prod.env FRONTEND_ENV_FILE=./env/frontend.prod.env EDGE_ENV_FILE=./env/edge.prod.env bash scripts/start-public-prod.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\start-public-prod.ps1 -BackendEnvFile ./env/backend.prod.env -FrontendEnvFile ./env/frontend.prod.env -EdgeEnvFile ./env/edge.prod.env
```

## 6. Verify The Public Test Deployment

Run the smoke verification against the generated hostnames:

```bash
FRONTEND_URL=https://app.203.0.113.10.sslip.io BACKEND_URL=https://api.203.0.113.10.sslip.io bash scripts/verify-deployment.sh
```

Windows PowerShell equivalent:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-deployment.ps1 -FrontendUrl https://app.203.0.113.10.sslip.io -BackendUrl https://api.203.0.113.10.sslip.io
```

Then manually verify:

- sign in
- dashboard loads
- runtime loads
- incidents load
- create a test order
- inventory reacts
- alerts and recommendations react
- realtime updates appear

## 7. What To Keep In Mind

- keep the generated `DB_PASSWORD` safe
- do not rotate that password casually after the first database initialization
- keep `SESSION_COOKIE_SECURE=true`
- keep `ALLOW_HEADER_FALLBACK=false`
- keep `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` when using the prod profile
- run Flyway validation before rollout
- prefer `SYNAPSECORE_REALTIME_BROKER_MODE=REDIS_PUBSUB` when the public test lane uses Redis
- use this deployment as a **public test lane**, not your final company production home

Before calling a public test lane pilot-ready, also run:

- `powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1`
- `npm.cmd run test:e2e:prod`

That keeps the public test lane tied to the same hosted proof contract as the real Render deployment.

## Best Short Description

If you want SynapseCore live on the internet without buying a domain yet, the fastest path is:

1. free Oracle VM
2. public IP
3. `sslip.io` or `nip.io` hostname generation
4. built-in SynapseCore public HTTPS stack
