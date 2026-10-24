# SynapseCore development pause checkpoint

**Decision:** Development is paused for the remainder of 2026. Resume only on the owner's explicit instruction in 2027. This is a pacing decision, not a cancellation, milestone reset, pilot approval, or change to engineering standards. No feature, investigation, deployment, hosted service, database, or infrastructure change is authorized by this checkpoint.

**Recorded:** 2026-10-24 12:18 UTC. This is a repository and GitHub checkpoint, not a fresh Render runtime assessment.

## Preserved baseline and current position

- GitHub `main` was verified with `git ls-remote` at `917c6a1aa7af59504838d2b83d53f3ab57b04648` (`Merge pull request #32 ... platform-login-live-gate`). This is the preserved shared source baseline. The primary local checkout is **not** on `main`: it is on `codex/platform-activity-slow-login-probe` at `e0fe8b6fa700663a12d838962d4ecf5bfeee4e8b`, with local changes listed below. Local `main` is at `adbf3098a2bbf8b415c4c4251d24b7658fe3690d` and is behind the verified remote tip; do not use it as the 2027 source baseline without fetching.
- [Master Engineering Readiness Map](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md): **M1 OPEN; M2 entry BLOCKED.** The current M1 sub-workstream is H1/H2/H7/H12 warm workspace latency and wait/SQL/resource attribution. Historical Hikari 10/10 ownership and workspace 204 client outcomes remain separate unresolved questions. H4 dispatch recovery and broader M1 exit criteria also remain open. This checkpoint neither closes nor reopens a gate.
- The last *recorded*, not freshly rechecked, backend Live observation in the map is merge `54b162e` on 2026-10-24 with a passing six-flag gate. That does not establish the Live revision or authenticated latency at 2027 resumption. See [Platform Owner login attribution](evidence/platform-owner-login-stage-attribution-2026-10-13.md) and [workspace incident composition](evidence/tenant-workspace-incident-composition-2026-10-15.md).

## Work in progress: do not discard or silently merge

GitHub search at checkpoint found one open PR: [draft PR #33](https://github.com/kobanihlulani77-maker/SynapsCore/pull/33), the company-week simulation on `codex/company-week-simulation` at `96623e30f005047f3f5c36f170f36a74b18e95c6`. Its exact-head CI run 699 passed. It was **not merged**; the report is product documentation, not M1 closure. Review or close it deliberately on resumption.

| Local checkout under `C:\Users\asus\Downloads\synapsecore_starter` | Recorded Git state and unfinished files |
| --- | --- |
| `synapsecore` | `codex/platform-activity-slow-login-probe` at `e0fe8b6...`; modified `frontend/Dockerfile`; untracked `.gitattributes`, two `docs/evidence/scenario-lifecycle-...md` files, and `recommendation-reconciliation-capture-20260905-181715.csv` / `single-slow-request-capture-20260905-220256.csv`. These are **not** part of `main` and were not committed for the pause. |
| `synapsecore-login-attribution` | `codex/workspace-telemetry-query-timing` at `917c6a1...`; modified `ConnectorTelemetryBatchReader.java` and its integration test. This is unfinished, uncommitted diagnostic/implementation work, **not** a verified fix. |
| `synapsecore-company-week` | Clean branch `codex/company-week-simulation` at `96623e3...`; pushed and represented by draft PR #33. |
| `synapsecore-dispatch-abort` | Clean older branch `codex/dispatch-abort-reclaim-proof` at `515c9b3...`; behind `origin/main`. |
| `synapsecore-investigation-protocol` | Clean older branch `codex/dispatch-post-publication-crash` at `ff251e6...`; behind `origin/main`. |
| `synapsecore-release-clean-3459f11` | Detached at `3459f11...`; untracked `frontend/.npm-cache/`. Git inspection required a per-command `safe.directory` exception because of different Windows ownership. The cache is regenerable and was not copied. |

The eight substantive modified/untracked files in the first two rows were copied, without moving or changing the originals, to the **local-only** `C:\Users\asus\.codex\synapsecore-pause-backup-2026-10-24\` tree. All eight copies matched their source SHA-256 hashes. The backup includes raw capture CSVs: keep it private, do not commit or share it, and arrange a separate secure machine backup if the PC may be replaced before 2027. Ignored proof-state credentials were not copied or exposed. No working tree was reset, cleaned, stashed, or archived.

## Where to resume in 2027

1. Reopen the repository at `C:\Users\asus\Downloads\synapsecore_starter\synapsecore`. Fetch and inspect `origin/main`, the worktree statuses above, and draft PR #33 before selecting a branch. Preserve the local-only files and backup until their relevance is decided; do not blindly apply or discard the unfinished connector changes.
2. Read [AGENTS.md](../AGENTS.md), the [Master Engineering Readiness Map](SYNAPSCORE-MASTER-ENGINEERING-READINESS-MAP.md), the [engineering investigation protocol](engineering-investigation-and-exit-protocol.md), and the linked [workspace evidence](evidence/tenant-workspace-incident-composition-2026-10-15.md). Reconcile them with the then-current source, CI, and Render revision; do not use this October snapshot as current runtime proof.
3. Resume **M1**, not M2: the recorded immediate causal discriminator is representative PostgreSQL plan/duration and DB/host resource evidence for residual warm workspace time, particularly connector telemetry and Scenario notification reads, before changing query shape or capacity. Separately determine the actual client outcome for historical slow/204 workspace requests. Keep historical Hikari-holder attribution and H4 recovery open until their own evidence gates pass.
4. Reopen Java 21, PowerShell, and the project's documented local/CI tooling as needed. Check Docker Desktop before disposable-PostgreSQL tests; it was unavailable during the October 24 company-week report, which used isolated H2 tests instead. Access to Render and any private database or Platform Owner credentials must be re-established through supported private channels, never from this document. Do not run hosted proof or mutate a tenant merely to re-establish the environment.

The read-only `synapsecore-weekday-control-brief` local automation was found **PAUSED** at this checkpoint. It was not changed. Hosted services, databases and plans were not inspected or altered for the pause.

**Restart rule:** Diagnose -> smallest justified change -> relevant local/CI/hosted proof -> record evidence -> gate decision. The 2027 schedule is not a substitute for any M-stage acceptance criterion.
