# Warehouse Context Phase 2 Evidence

## Scope

This phase validates browser currentness, bounded authority refresh, page-local
selection safety, async request handling, session lifecycle, and the relationship
between warehouse options and existing tenant-level realtime delivery. It does not
add a global warehouse selector or redesign the websocket topic model.

Phase 1 revision: `198c236b9a9d7851630953f115a6b5184f90b347`

## Product Contract

SynapseCore uses aggregate tenant-wide or scope-filtered reads. Only workflows that
need a warehouse target expose a page-local selector or carry an explicit
`warehouseCode`. The authenticated `/api/warehouses` response is the operation-option
authority: it is tenant-bound, active-only, and scope-filtered. Settings may still
display retired records for administration.

No warehouse authority is stored in local storage, URLs, or a global frontend
selection. The backend remains authoritative during every stale interval.

## Browser Bootstrap and Currentness

The frontend refreshes `/api/auth/session` and `/api/warehouses` during authenticated
workspace bootstrap and at one bounded 30-second interval. Role and scope state is
replaced only when the authoritative session differs. Active operation options are
rebuilt from the returned warehouse list, so a retired or removed warehouse cannot
be reintroduced by inventory data, administrative history, or stale scope data.

Refresh requests are cancelled when superseded or when the workspace unmounts.
Request-version checks prevent an older response from committing after a newer
authority refresh. A failed refresh fails closed for warehouse-dependent actions and
shows an authority-read failure rather than presenting broader access.

## Page-Local Workflow

Scenario is the representative warehouse-dependent page. Its selected value is
visible, changeable, and submitted explicitly. Preview and save are disabled unless
the selected target remains in the current active authorized option set. If the
target becomes invalid, the page does not silently submit another warehouse.

Settings remains an administrative surface. Its active/retired selection cannot feed
Scenario or other operational forms because it is separate page-local state.

Aggregate Dashboard, Orders, Inventory, Alerts, Recommendations, and Fulfillment
behavior remains unchanged.

## Lifecycle Matrix

| Situation | Browser result | Authority result |
| --- | --- | --- |
| Scoped reload | Only active assigned warehouses appear | Server session and `/api/warehouses` decide |
| Scope removed | Removed warehouse disappears on refresh; stale request is not retried | Backend denies immediately |
| Final scope removed | Warehouse-required Scenario actions become unavailable | Empty scope is not synthesized as tenant-wide by the frontend |
| Scope added | Warehouse appears after authoritative refresh | No automatic scope assignment occurs |
| Warehouse retired | Removed from active operation options | Backend rejects new inactive targeting |
| Warehouse reactivated | Eligible again only when tenant-wide or already assigned | Reactivation does not grant scope |
| Stale option submit | Action is blocked or reports controlled authority failure | No silent replacement target |
| Refresh failure | Options fail closed and explain that access cannot be confirmed | No authority broadening |
| Refresh recovery | Next successful response restores only current active authorized options | No re-login required |
| Logout or user switch | Workspace and warehouse access state are cleared | Session boundary remains authoritative |
| Tenant switch | New tenant warehouse response replaces old state | Tenant identity remains server-bound |

## Async and Realtime Findings

Scenario selection changes are synchronous form-state changes; they do not launch a
warehouse-dependent asynchronous read. Therefore there is no current Scenario
response race that can paint warehouse A under warehouse B. The action gates still
check the current active option set before preview or save.

The existing realtime client remains tenant-topic based. Warehouse Context does not
depend on a warehouse-specific subscription switch. Authority refresh is REST-based,
and existing backend realtime authorization remains the final boundary. A realtime
disconnect therefore does not broaden warehouse options; the bounded REST refresh
continues to converge them.

Exact rendered timing for live scope removal, live retirement, multi-tab authority
change, and provider outage remains a hosted/browser evidence concern rather than a
new permission model.

## Verification Performed

- Frontend `npm.cmd run verify`: passed; lint and production build passed.
- Active-option executable check: passed; retired and unauthorized codes were excluded while active authorized codes remained.
- Documentation link check: passed; 778 local markdown links checked.
- `git diff --check`: passed; line-ending warnings were non-fatal Git normalization warnings.
- Backend suite: not rerun because no backend production code changed.
- Hosted proof: requires deployment of this frontend revision before final live evidence.

## A/B/C/D Classification

| Class | Result |
| --- | --- |
| A: required authority capability | `0` identified after the Phase 2 implementation |
| B: intentional boundary | No global selector, aggregate pages, page-local Scenario/Settings state, no URL or persisted warehouse authority, bounded REST convergence |
| C: evidence gap | Hosted rendered scope removal/retirement timing, multi-tab timing, and provider-outage observation |
| D: future extension | Global selector, favorites, URL context, recent locations, or comparison workspace only if pilot evidence requires them |

## Final Gate

Phase 2 is implementation-complete locally with no identified Classification A
authority defect. Final hosted/browser acceptance must be recorded after the pushed
revision is deployed and the live connection gate is healthy. No Phase 3 or Activity
/Audit work is included here.
