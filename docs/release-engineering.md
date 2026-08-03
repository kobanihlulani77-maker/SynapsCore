# Release Engineering

This guide defines how SynapseCore releases should be prepared, verified, documented, and rolled back.

## Versioning Strategy

Recommended current strategy:

- `v0.x.y` for pilot-era releases
- `-pilot-rcN` for pilot release candidates
- `-hotfix.N` only when a release needs urgent correction

Current proposed pilot RC:

- `v0.9.0-pilot-rc1`

Do not tag a release until the committed state passes the relevant readiness check.

## Release Candidate Process

Pilot RC steps:

1. freeze product scope
2. confirm evidence baseline
3. update pilot RC docs
4. run docs link check
5. run frontend verify
6. run live connection gate
7. run `scripts\pilot-rc-check.ps1`
8. confirm `PILOT_RC_READY=True`
9. create annotated tag only after approval

## Production Release Process

Production release requires:

- release candidate evidence
- current hosted proof evidence if runtime behavior changed
- release notes
- rollback plan
- support readiness
- environment variable review
- backup/restore posture review

Production release is not the same as a pilot RC.

## Hotfix Process

Hotfix conditions:

- security issue
- readiness failure caused by release
- proof blocker in supported flow
- replay inconsistency
- auth/session regression
- severe operator-blocking UI regression

Hotfix rules:

- fix the smallest real seam
- do not add features
- preserve evidence
- run targeted verification
- rerun hosted proof when proof-covered behavior changed
- document residual risk

## Rollback Strategy

Rollback options:

- revert deployment to previous known-good commit
- pause pilot activity
- disable affected connector lane
- return affected workflow to existing system of record
- preserve evidence before cleanup

Rollback should be rehearsed, not improvised.

## Release Evidence

Evidence should include:

- version/tag
- commit hash
- proof result
- frontend verify result
- backend test result if backend changed
- live connection classification
- known exclusions
- support contact/process
- rollback notes

Evidence location:

- `docs\release-evidence-*.md`

## Release Documentation

Update:

- `README.md`
- `docs\verification-status.md`
- `docs\hosted-proof.md`
- `docs\release-evidence-*.md`
- `docs\pilot-release-candidate.md` for pilot RCs
- `docs\INDEX.md`

Do not leave old status language near current proof claims.

## Tagging

Proposed tag command:

```powershell
git tag -a v0.9.0-pilot-rc1 -m "SynapseCore pilot release candidate 1"
git push origin v0.9.0-pilot-rc1
```

Do not run the tag command until approved.
