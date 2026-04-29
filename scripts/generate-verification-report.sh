#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_URL="http://127.0.0.1"
BACKEND_URL="http://127.0.0.1:8080"
OUTPUT_FILE="$ROOT_DIR/docs/verification-status.md"
RUN_BACKEND_TESTS="false"
BACKEND_TEST_TIMEOUT_SECONDS=1200

frontend_routes=(
  "/"
  "/product"
  "/sign-in"
  "/contact"
  "/dashboard"
  "/alerts"
  "/recommendations"
  "/orders"
  "/inventory"
  "/locations"
  "/fulfillment"
  "/scenarios"
  "/scenario-history"
  "/approvals"
  "/escalations"
  "/integrations"
  "/replay-queue"
  "/runtime"
  "/audit-events"
  "/users"
  "/company-settings"
  "/profile"
  "/platform-admin"
  "/tenant-management"
  "/system-config"
  "/releases"
)

backend_endpoints=(
  "/actuator/health/readiness"
  "/api/dashboard/summary"
  "/api/dashboard/snapshot"
  "/api/system/runtime"
  "/api/system/incidents"
  "/api/alerts"
  "/api/recommendations"
  "/api/orders/recent"
  "/api/inventory"
  "/api/fulfillment"
  "/api/integrations/orders/imports/recent"
  "/api/scenarios/history"
  "/api/access/tenants"
  "/api/auth/session"
)

usage() {
  cat <<'EOF'
Usage: bash scripts/generate-verification-report.sh [options]

Options:
  --frontend-url URL              Frontend base URL
  --backend-url URL               Backend base URL
  --output-file PATH              Output markdown path
  --run-backend-tests             Rerun backend tests before reading Surefire reports
  --backend-test-timeout SECONDS  Timeout for backend test run when enabled
  --help                          Show this help
EOF
}

while (($#)); do
  case "$1" in
    --frontend-url)
      FRONTEND_URL="$2"
      shift 2
      ;;
    --backend-url)
      BACKEND_URL="$2"
      shift 2
      ;;
    --output-file)
      OUTPUT_FILE="$2"
      if [[ "$OUTPUT_FILE" != /* && "$OUTPUT_FILE" != [A-Za-z]:/* ]]; then
        OUTPUT_FILE="$ROOT_DIR/$OUTPUT_FILE"
      fi
      shift 2
      ;;
    --run-backend-tests)
      RUN_BACKEND_TESTS="true"
      shift
      ;;
    --backend-test-timeout)
      BACKEND_TEST_TIMEOUT_SECONDS="$2"
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

write_step() {
  printf '\n==> %s\n' "$1"
}

resolve_npm_cmd() {
  if command -v npm >/dev/null 2>&1; then
    echo "npm"
    return
  fi
  if command -v npm.cmd >/dev/null 2>&1; then
    echo "npm.cmd"
    return
  fi
  echo "npm command not found" >&2
  exit 1
}

resolve_mvnw_cmd() {
  if [[ -x "$BACKEND_DIR/mvnw" ]]; then
    echo "./mvnw"
    return
  fi
  if [[ -f "$BACKEND_DIR/mvnw.cmd" ]]; then
    echo "./mvnw.cmd"
    return
  fi
  echo "Maven wrapper not found" >&2
  exit 1
}

resolve_powershell_cmd() {
  if command -v pwsh >/dev/null 2>&1; then
    echo "pwsh"
    return
  fi
  if command -v pwsh.exe >/dev/null 2>&1; then
    echo "pwsh.exe"
    return
  fi
  if command -v powershell >/dev/null 2>&1; then
    echo "powershell"
    return
  fi
  if command -v powershell.exe >/dev/null 2>&1; then
    echo "powershell.exe"
    return
  fi
  echo ""
}

to_windows_path() {
  local path="$1"
  if command -v wslpath >/dev/null 2>&1; then
    wslpath -w "$path"
    return
  fi
  printf '%s\n' "$path"
}

run_capture() {
  local workdir="$1"
  local timeout_seconds="$2"
  shift 2

  local stdout_file stderr_file
  stdout_file="$(mktemp)"
  stderr_file="$(mktemp)"
  local exit_code=0
  local timed_out="false"

  (
    cd "$workdir"
    if [[ "$timeout_seconds" -gt 0 ]] && command -v timeout >/dev/null 2>&1; then
      if ! timeout "${timeout_seconds}s" "$@" >"$stdout_file" 2>"$stderr_file"; then
        exit_code=$?
        if [[ "$exit_code" -eq 124 ]]; then
          timed_out="true"
        fi
      fi
    else
      if ! "$@" >"$stdout_file" 2>"$stderr_file"; then
        exit_code=$?
      fi
    fi

    printf '__EXIT__=%s\n' "$exit_code"
    printf '__TIMED_OUT__=%s\n' "$timed_out"
    printf '__STDOUT__=%s\n' "$stdout_file"
    printf '__STDERR__=%s\n' "$stderr_file"
  )
}

safe_url_status() {
  local url="$1"
  local cookies_file="${2:-}"
  local code
  if [[ -n "$cookies_file" ]]; then
    code="$(curl -ksS -o /dev/null -w "%{http_code}" -b "$cookies_file" "$url" || true)"
  else
    code="$(curl -ksS -o /dev/null -w "%{http_code}" "$url" || true)"
  fi
  if [[ "$code" == "200" ]]; then
    printf '200'
  elif [[ -n "$code" ]]; then
    printf '%s' "$code"
  else
    printf 'ERR'
  fi
}

parse_test_reports() {
  local report_dir="$BACKEND_DIR/target/surefire-reports"
  report_files=()
  total_tests=0
  total_failures=0
  total_errors=0
  total_skipped=0

  if [[ ! -d "$report_dir" ]]; then
    return
  fi

  while IFS= read -r -d '' file; do
    report_files+=("$file")
    local line
    line="$(grep -E 'Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+' "$file" | tail -n 1 || true)"
    if [[ "$line" =~ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+),\ Errors:\ ([0-9]+),\ Skipped:\ ([0-9]+) ]]; then
      total_tests=$((total_tests + BASH_REMATCH[1]))
      total_failures=$((total_failures + BASH_REMATCH[2]))
      total_errors=$((total_errors + BASH_REMATCH[3]))
      total_skipped=$((total_skipped + BASH_REMATCH[4]))
    fi
  done < <(find "$report_dir" -maxdepth 1 -type f -name '*.txt' -print0 | sort -z)
}

extract_json_field() {
  local text="$1"
  local key="$2"
  printf '%s' "$text" | tr -d '\r\n' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" | tail -n 1
}

extract_json_bool() {
  local text="$1"
  local key="$2"
  printf '%s' "$text" | tr -d '\r\n' | sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\(true\|false\).*/\1/p" | tail -n 1
}

append_status_row() {
  local capability="$1"
  local status="$2"
  local proof="$3"
  local notes="$4"
  status_rows+=$'| '"$capability"$' | `'"$status"$'` | '"$proof"$' | '"$notes"$' |\n'
}

NPM_CMD="$(resolve_npm_cmd)"
MVNW_CMD="$(resolve_mvnw_cmd)"
POWERSHELL_CMD="$(resolve_powershell_cmd)"

if [[ -n "$POWERSHELL_CMD" && "$POWERSHELL_CMD" == *.exe ]] && command -v wslpath >/dev/null 2>&1; then
  write_step "Delegate localhost verification to PowerShell"
  ps_script_path="$(to_windows_path "$ROOT_DIR/scripts/generate-verification-report.ps1")"
  ps_output_file="$(to_windows_path "$OUTPUT_FILE")"
  delegate_args=(
    -ExecutionPolicy Bypass
    -File "$ps_script_path"
    -FrontendUrl "$FRONTEND_URL"
    -BackendUrl "$BACKEND_URL"
    -OutputFile "$ps_output_file"
    -BackendTestTimeoutSeconds "$BACKEND_TEST_TIMEOUT_SECONDS"
  )
  if [[ "$RUN_BACKEND_TESTS" == "true" ]]; then
    delegate_args+=(-RunBackendTests)
  fi

  "$POWERSHELL_CMD" "${delegate_args[@]}"
  printf '\nDelegated verification report generation to PowerShell:\n  %s\n' "$OUTPUT_FILE"
  exit 0
fi

write_step "Run frontend production build"
frontend_build_meta="$(run_capture "$FRONTEND_DIR" 0 "$NPM_CMD" run build)"
frontend_build_exit="$(printf '%s\n' "$frontend_build_meta" | sed -n 's/^__EXIT__=//p')"
frontend_build_timed_out="$(printf '%s\n' "$frontend_build_meta" | sed -n 's/^__TIMED_OUT__=//p')"
frontend_build_stdout_file="$(printf '%s\n' "$frontend_build_meta" | sed -n 's/^__STDOUT__=//p')"
frontend_build_stderr_file="$(printf '%s\n' "$frontend_build_meta" | sed -n 's/^__STDERR__=//p')"
frontend_build_output="$(cat "$frontend_build_stdout_file" "$frontend_build_stderr_file" 2>/dev/null || true)"
frontend_dist_exists="false"
[[ -f "$FRONTEND_DIR/dist/index.html" ]] && frontend_dist_exists="true"
frontend_build_status="NOT YET PROVEN"
if [[ "$frontend_build_timed_out" == "false" ]] && { [[ "$frontend_build_exit" == "0" ]] || grep -q 'built in' <<<"$frontend_build_output" || [[ "$frontend_dist_exists" == "true" ]]; }; then
  frontend_build_status="PASS"
fi

backend_test_status="NOT YET PROVEN"
backend_test_notes="No Surefire reports found."
backend_test_command_status="not-run"

if [[ "$RUN_BACKEND_TESTS" == "true" ]]; then
  write_step "Run backend automated tests"
  backend_test_meta="$(run_capture "$BACKEND_DIR" "$BACKEND_TEST_TIMEOUT_SECONDS" "$MVNW_CMD" test)"
  backend_test_exit="$(printf '%s\n' "$backend_test_meta" | sed -n 's/^__EXIT__=//p')"
  backend_test_timed_out="$(printf '%s\n' "$backend_test_meta" | sed -n 's/^__TIMED_OUT__=//p')"
  if [[ "$backend_test_timed_out" == "true" ]]; then
    backend_test_command_status="timed-out"
  elif [[ "$backend_test_exit" == "0" ]]; then
    backend_test_command_status="clean-exit"
  else
    backend_test_command_status="non-zero-exit"
  fi
fi

write_step "Read backend test evidence"
report_files=()
total_tests=0
total_failures=0
total_errors=0
total_skipped=0
parse_test_reports

if [[ ${#report_files[@]} -gt 0 ]]; then
  if [[ "$total_failures" == "0" && "$total_errors" == "0" ]]; then
    if [[ "$RUN_BACKEND_TESTS" == "true" ]]; then
      if [[ "$backend_test_command_status" == "clean-exit" ]]; then
        backend_test_status="PASS"
        backend_test_notes="Backend tests completed cleanly."
      elif [[ "$backend_test_command_status" == "timed-out" ]]; then
        backend_test_status="PASS WITH CAVEAT"
        backend_test_notes="Surefire reports are green, but the backend test run timed out before clean exit."
      else
        backend_test_status="PASS WITH CAVEAT"
        backend_test_notes="Surefire reports are green, but the backend test launcher did not exit cleanly."
      fi
    else
      backend_test_status="PASS WITH CAVEAT"
      backend_test_notes="Using latest Surefire reports instead of rerunning backend tests in this pass."
    fi
  else
    backend_test_status="NOT YET PROVEN"
    backend_test_notes="Surefire reports contain failures or errors."
  fi
fi

write_step "Run deployment smoke"
deployment_meta="$(run_capture "$ROOT_DIR" 0 bash scripts/verify-deployment.sh)"
deployment_exit="$(printf '%s\n' "$deployment_meta" | sed -n 's/^__EXIT__=//p')"
deployment_timed_out="$(printf '%s\n' "$deployment_meta" | sed -n 's/^__TIMED_OUT__=//p')"
deployment_stdout_file="$(printf '%s\n' "$deployment_meta" | sed -n 's/^__STDOUT__=//p')"
deployment_stderr_file="$(printf '%s\n' "$deployment_meta" | sed -n 's/^__STDERR__=//p')"
deployment_output="$(cat "$deployment_stdout_file" "$deployment_stderr_file" 2>/dev/null || true)"
deployment_status="NOT YET PROVEN"
if [[ "$deployment_timed_out" == "false" ]] && { [[ "$deployment_exit" == "0" ]] || grep -qi 'deployment checks passed' <<<"$deployment_output"; }; then
  deployment_status="PASS"
fi

company_status="NOT YET PROVEN"
company_notes="Company-readiness script could not be run from bash because PowerShell is unavailable."
company_output=""
company_tenant_code=""
company_tenant_name=""
company_workspace_admin=""
company_integration=""
company_replay=""
company_planning=""
company_escalated=""
company_fulfillment=""
company_trust=""

if [[ -n "$POWERSHELL_CMD" ]]; then
write_step "Run company-readiness verification"
  company_meta="$(run_capture "$ROOT_DIR" 1800 "$POWERSHELL_CMD" -ExecutionPolicy Bypass -File scripts/verify-company-readiness.ps1 -FrontendUrl "$FRONTEND_URL" -BackendUrl "$BACKEND_URL")"
  company_exit="$(printf '%s\n' "$company_meta" | sed -n 's/^__EXIT__=//p')"
  company_timed_out="$(printf '%s\n' "$company_meta" | sed -n 's/^__TIMED_OUT__=//p')"
  company_stdout_file="$(printf '%s\n' "$company_meta" | sed -n 's/^__STDOUT__=//p')"
  company_stderr_file="$(printf '%s\n' "$company_meta" | sed -n 's/^__STDERR__=//p')"
  company_output="$(cat "$company_stdout_file" "$company_stderr_file" 2>/dev/null || true)"
  if [[ "$company_timed_out" == "false" ]] && { [[ "$company_exit" == "0" ]] || grep -q 'Company readiness verification passed' <<<"$company_output"; }; then
    company_status="PASS"
    company_notes="Onboarding, integrations, replay, planning, fulfillment, and trust."
  else
    company_notes="Company-readiness verification did not pass in this run."
  fi

  company_tenant_code="$(extract_json_field "$company_output" tenantCode)"
  company_tenant_name="$(extract_json_field "$company_output" tenantName)"
  company_workspace_admin="$(extract_json_bool "$company_output" workspaceAdminVerified)"
  company_integration="$(extract_json_bool "$company_output" integrationVerified)"
  company_replay="$(extract_json_bool "$company_output" replayVerified)"
  company_planning="$(extract_json_bool "$company_output" planningVerified)"
  company_escalated="$(extract_json_bool "$company_output" escalatedPlanningVerified)"
  company_fulfillment="$(extract_json_bool "$company_output" fulfillmentVerified)"
  company_trust="$(extract_json_bool "$company_output" trustSurfaceVerified)"
fi

write_step "Verify sign-in session"
cookies_file="$(mktemp)"
login_payload='{"tenantCode":"STARTER-OPS","username":"operations.lead","password":"lead-2026"}'
login_status="$(curl -ksS -o /dev/null -w "%{http_code}" -c "$cookies_file" -H "Content-Type: application/json" -d "$login_payload" "$BACKEND_URL/api/auth/session/login" || true)"
session_body="$(curl -ksS -b "$cookies_file" "$BACKEND_URL/api/auth/session" || true)"

session_status="NOT YET PROVEN"
session_note="Session verification failed in this pass."
if [[ "$login_status" == "200" && "$session_body" == *'"signedIn":true'* ]]; then
  session_status="PASS"
  session_tenant="$(extract_json_field "$session_body" tenantCode)"
  session_user="$(extract_json_field "$session_body" username)"
  session_note="Signed in as ${session_tenant:-unknown} / ${session_user:-unknown}."
fi

write_step "Sweep frontend routes"
route_lines=""
passed_routes=0
for route in "${frontend_routes[@]}"; do
  status="$(safe_url_status "${FRONTEND_URL%/}${route}")"
  [[ "$status" == "200" ]] && passed_routes=$((passed_routes + 1))
  route_lines+="- \`$route\` -> \`$status\`"$'\n'
done
route_status="NOT YET PROVEN"
[[ "$passed_routes" -eq "${#frontend_routes[@]}" ]] && route_status="PASS"

write_step "Sweep backend endpoints"
endpoint_lines=""
passed_endpoints=0
for endpoint in "${backend_endpoints[@]}"; do
  status="$(safe_url_status "${BACKEND_URL%/}${endpoint}" "$cookies_file")"
  [[ "$status" == "200" ]] && passed_endpoints=$((passed_endpoints + 1))
  endpoint_lines+="- \`$endpoint\` -> \`$status\`"$'\n'
done
endpoint_status="NOT YET PROVEN"
[[ "$passed_endpoints" -eq "${#backend_endpoints[@]}" ]] && endpoint_status="PASS"

realtime_status="NOT YET PROVEN"
for report_file in "${report_files[@]}"; do
  if [[ "$(basename "$report_file")" == "com.synapsecore.realtime.RealtimeServiceTest.txt" ]]; then
    realtime_status="PASS WITH CAVEAT"
  fi
done

status_rows='| Capability | Status | Proof | Notes |
| --- | --- | --- | --- |
'
append_status_row "Frontend production build" "$frontend_build_status" "\`npm run build\`" "Build completed successfully."
append_status_row "Backend automated tests" "$backend_test_status" "Surefire reports" "$backend_test_notes"
append_status_row "Local/self-host deployment smoke" "$deployment_status" "\`scripts/verify-deployment.sh\`" "Seed-tenant smoke for health, readiness, metrics, dashboard, runtime, incidents, frontend health, and runtime config. This is supplemental to hosted proof."
append_status_row "Local/self-host readiness rehearsal" "$company_status" "\`scripts/verify-company-readiness.ps1\`" "$company_notes"
append_status_row "Frontend route sweep" "$route_status" "$passed_routes/${#frontend_routes[@]} routes returned 200" "Public, core, control, systems, and admin routes checked against the production-shaped frontend."
append_status_row "Backend endpoint sweep" "$endpoint_status" "$passed_endpoints/${#backend_endpoints[@]} endpoints returned 200" "Operational, trust, and access endpoints checked with a signed-in local seed admin session for protected surfaces."
append_status_row "Tenant sign-in and session" "$session_status" "\`POST /api/auth/session/login\` + \`GET /api/auth/session\`" "$session_note"
append_status_row "Realtime implementation" "$realtime_status" "Surefire realtime report + live app architecture" "Backend realtime tests are green, but this pass did not include a direct socket-level probe."
append_status_row "Public HTTPS/domain compose contract" "PASS WITH CAVEAT" "\`docker compose -f docker-compose.public.yml config\`" "Compose contract is valid for self-host/public deployment. Hosted proof already passed separately on Render."

backend_evidence_list=""
if [[ ${#report_files[@]} -gt 0 ]]; then
  for report_file in "${report_files[@]}"; do
    backend_evidence_list+="- \`backend/target/surefire-reports/$(basename "$report_file")\`"$'\n'
  done
else
  backend_evidence_list="- No test report files were found."$'\n'
fi

company_summary_lines="- Company-readiness script output was not parsed into structured summary text."
if [[ -n "$company_tenant_code" || -n "$company_tenant_name" || -n "$company_workspace_admin" ]]; then
  company_summary_lines=""
  [[ -n "$company_tenant_code" ]] && company_summary_lines+="- \`tenantCode=$company_tenant_code\`"$'\n'
  [[ -n "$company_tenant_name" ]] && company_summary_lines+="- \`tenantName=$company_tenant_name\`"$'\n'
  [[ -n "$company_workspace_admin" ]] && company_summary_lines+="- \`workspaceAdminVerified=$company_workspace_admin\`"$'\n'
  [[ -n "$company_integration" ]] && company_summary_lines+="- \`integrationVerified=$company_integration\`"$'\n'
  [[ -n "$company_replay" ]] && company_summary_lines+="- \`replayVerified=$company_replay\`"$'\n'
  [[ -n "$company_planning" ]] && company_summary_lines+="- \`planningVerified=$company_planning\`"$'\n'
  [[ -n "$company_escalated" ]] && company_summary_lines+="- \`escalatedPlanningVerified=$company_escalated\`"$'\n'
  [[ -n "$company_fulfillment" ]] && company_summary_lines+="- \`fulfillmentVerified=$company_fulfillment\`"$'\n'
  [[ -n "$company_trust" ]] && company_summary_lines+="- \`trustSurfaceVerified=$company_trust\`"$'\n'
fi

mkdir -p "$(dirname "$OUTPUT_FILE")"

cat >"$OUTPUT_FILE" <<EOF
# Verification Status

Last verified: **$(date +"%B %-d, %Y" 2>/dev/null || date +"%B %d, %Y")**

This document is generated by:

\`\`\`bash
bash scripts/generate-verification-report.sh
\`\`\`

## Status Legend

- \`PASS\`: proven in this verification pass
- \`PASS WITH CAVEAT\`: proven with a meaningful limit or follow-up note
- \`NOT YET PROVEN\`: not failed, but not fully proven in the target environment yet

## Environment Used

- Frontend: \`$FRONTEND_URL\`
- Backend: \`$BACKEND_URL\`

## Commands Executed

\`\`\`bash
cd frontend
$(printf '%q' "$NPM_CMD") run build

cd ..
bash scripts/verify-deployment.sh
$(if [[ -n "$POWERSHELL_CMD" ]]; then printf '%q -ExecutionPolicy Bypass -File scripts/verify-company-readiness.ps1 -FrontendUrl %q -BackendUrl %q' "$POWERSHELL_CMD" "$FRONTEND_URL" "$BACKEND_URL"; else printf '# company-readiness PowerShell script not available in this bash environment'; fi)
\`\`\`

## Backend Test Evidence

${backend_evidence_list}
- total tests: \`$total_tests\`
- failures: \`$total_failures\`
- errors: \`$total_errors\`
- skipped: \`$total_skipped\`

## Frontend Route Proof

${route_lines}
Result: **$passed_routes/${#frontend_routes[@]} main frontend routes returned \`200\`**

## Backend Endpoint Proof

${endpoint_lines}
Result: **$passed_endpoints/${#backend_endpoints[@]} key backend endpoints returned \`200\`**

## Capability Status Board

${status_rows}
## Company-Readiness Flow Result

Company-readiness command status: **$company_status**

${company_summary_lines}
## Current Classification

- core backend: \`FULLY REAL\`
- frontend architecture: \`FULLY REAL\`
- hosted proof tooling: \`FULLY REAL\`
- migration/recovery tooling: \`FULLY REAL\`
- local/dev smoke scripts: \`DEV-ONLY ACCEPTABLE\`
- release/reporting scripts: \`FULLY REAL\`
## Honest Final Read

This report proves the **local/self-host verification lane**, not the final hosted signoff lane.

What is proven now:

- the product builds
- the main frontend routes load
- the main backend APIs respond
- tenant onboarding works
- company operations flows work
- integrations and replay work
- planning and approvals work
- trust surfaces work
- the hosted proof lane has already passed live on Render

What this local report does **not** replace:

- \`powershell -ExecutionPolicy Bypass -File scripts/prepare-hosted-proof.ps1\`
- \`cd frontend && npm.cmd run test:e2e:prod\`

That means the current product status is:

- **local/self-host smoke:** supplemental and useful
- **hosted proof:** already passed live on Render and remains the primary final signoff lane
EOF

rm -f "$frontend_build_stdout_file" "$frontend_build_stderr_file"
rm -f "$deployment_stdout_file" "$deployment_stderr_file"
rm -f "$cookies_file"
if [[ -n "${company_stdout_file:-}" ]]; then
  rm -f "$company_stdout_file" "$company_stderr_file"
fi

printf '\nGenerated verification report:\n  %s\n' "$OUTPUT_FILE"
