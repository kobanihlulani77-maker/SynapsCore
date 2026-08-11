# Backup And Restore Runbook

This runbook explains how SynapseCore should treat PostgreSQL backup, restore, and restore-drill work.

It is intentionally operational. Backup and restore are not marketing claims. They are trust controls for the database that holds tenant workspace state, catalog data, inventory posture, orders, replay records, audit logs, runtime-supporting operational records, scenarios, and approvals.

## Current Scope

SynapseCore currently treats PostgreSQL as the operational record of truth.

Current backup-related assets include:

- `scripts/backup-postgres.ps1`
- `scripts/backup-postgres.sh`
- `scripts/restore-postgres.ps1`
- `scripts/restore-postgres.sh`
- `scripts/verify-restore-drill.ps1`

These scripts support backup/restore discipline, but they do not replace a full enterprise backup platform, managed retention policy, cross-region replication, or formal disaster-recovery program.

## Why This Matters

If PostgreSQL is lost or restored incorrectly, the impact can include:

- lost tenant workspace records
- incorrect inventory or order state
- replay queue gaps
- missing audit evidence
- stale approval/scenario state
- misleading dashboard snapshots
- hosted proof failure
- loss of runtime trust

For SynapseCore, database recovery is product recovery.

## Backup Principles

Use these principles for any serious environment:

- Create a backup before backend releases that include schema or migration changes.
- Create a backup before manually touching operational data.
- Store backups outside the running database container or transient runtime filesystem.
- Label backups with environment, timestamp, commit, and operator.
- Never treat a backup as valid until a restore drill has proven it can be read.
- Do not run hosted proof as proof of backup integrity; proof validates app behavior, not backup recoverability.

## Restore Principles

Restore should be deliberate because it can overwrite newer operational truth.

Before restore, confirm:

- which environment is being restored
- which tenant data is affected
- why restore is safer than forward repair
- what data will be lost after the backup timestamp
- whether the backend should be stopped during restore
- whether the restored schema matches the application commit
- how operators will be informed

## Recovery Classifications

| Classification | Meaning | Typical Action |
|---|---|---|
| `BACKUP_AVAILABLE` | A backup artifact exists | Verify metadata and storage path |
| `BACKUP_VERIFIED` | Backup has been restored in a drill or test target | Treat as recovery-capable |
| `RESTORE_REQUIRED` | Forward repair is unsafe or impossible | Stop write traffic and restore carefully |
| `RESTORE_RISK_HIGH` | Restore may lose accepted operational work | Escalate before proceeding |
| `RESTORE_COMPLETE` | Database is restored and backend starts against it | Run readiness/auth/ws checks |
| `PROOF_READY_AFTER_RESTORE` | Readiness, auth, websocket, and session behavior are healthy | Hosted proof may resume |

## Suggested Backup Sequence

PowerShell:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\backup-postgres.ps1
```

Bash:

```bash
cd /path/to/synapsecore
bash scripts/backup-postgres.sh
```

Expected result:

- backup file created
- backup path printed
- backup size printed
- backup SHA256 checksum printed
- no secret values printed
- command exits successfully

If the script fails:

- confirm database host and credentials
- confirm Docker or host Postgres is reachable
- confirm the backup destination exists and is writable
- do not proceed with release until backup posture is understood

## Suggested Restore Drill Sequence

PowerShell:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\verify-restore-drill.ps1
```

Expected result:

- restore drill target is prepared
- backup is restored into the drill target
- schema can be inspected after restore
- source and restored Flyway state match
- source and restored operational counts match
- deterministic aggregate hashes match for critical tenant, user, catalog, inventory, replay, and scenario data
- script exits successfully

A restore drill should never casually overwrite the active local or live database. If a script asks for a target, verify the target before continuing.

## Full Restore Sequence

Use this only when restore has been approved.

1. Pause proof and user-facing validation.
2. Stop backend write traffic if possible.
3. Confirm backup file identity and timestamp.
4. Confirm the restore target printed by the script.
5. Run the restore script.
6. Start backend.
7. Verify health/readiness/auth/websocket.
8. Check dashboard snapshot and runtime incidents.
9. Run hosted proof only after `PROOF_ALLOWED=True`.

PowerShell live readiness after restore:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Local readiness after restore:

```powershell
cd C:\Users\asus\Downloads\synapsecore_starter\synapsecore
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
```

## RPO And RTO Posture

Current honest posture:

- Formal RPO and RTO targets are not yet enterprise-contract commitments.
- Pilot deployments should define backup cadence and restore expectations before operational reliance.
- Enterprise adoption should harden backup scheduling, restore drills, retention, alerting, and operator approval flow.

Recommended pilot targets:

- RPO: define based on accepted operational risk and order velocity.
- RTO: define based on whether SynapseCore is advisory, operationally active, or mission-critical for the pilot.

## When Restore Is Unsafe

Do not restore casually when:

- the issue is only frontend routing or browser cache
- backend code rollback is sufficient
- the backup is older than accepted operational work that cannot be lost
- the schema version does not match the intended application commit
- the restore target is unclear
- operators have not been warned about possible data loss

## What To Check After Restore

Minimum checks:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

If local:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-local-connections.ps1
```

Then verify:

- health returns `UP`
- readiness returns `UP`
- auth/session endpoint responds
- websocket info responds
- dashboard snapshot responds
- replay queue is visible
- runtime incidents make sense
- operators can sign in

## Hosted Proof After Restore

Hosted proof should run only after:

- frontend is reachable
- backend is reachable
- DB readiness is healthy
- auth/session responds
- websocket info responds
- `PROOF_ALLOWED=True`

Commands:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\check-live-connections.ps1
```

Only if proof is allowed:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\prepare-hosted-proof.ps1

cd frontend
npm.cmd run test:e2e:prod
```

## Related Docs

- [database-and-migrations.md](database-and-migrations.md)
- [deployment-recovery-guide.md](deployment-recovery-guide.md)
- [render-recovery-playbook.md](render-recovery-playbook.md)
- [release-process.md](release-process.md)
- [current-limitations.md](current-limitations.md)

## Bottom Line

SynapseCore should never claim operational trust after a database incident until restore posture is classified, readiness is back, and proof is allowed again.
