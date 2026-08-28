# Catalog Lifecycle Phase 1 Evidence

**Scope:** Catalog / Products authority, tenant ownership, identity, validation, and basic side-effect boundaries.

**Verification date:** 2026-08-28

## Starting State

- Starting HEAD: `1de2bb4872e4926ffbc09e87968953e1f29c5eb2`
- Runtime code was not changed.
- Existing unrelated worktree changes were preserved and were not staged.
- Focused tests ran against isolated H2 test fixtures, not the hosted deployment.

## Authority Matrix

| Actor | Read Catalog | Create | Update | Product CSV import | Evidence |
| --- | --- | --- | --- | --- | --- |
| `TENANT_ADMIN` | Yes | Allowed | Allowed | Allowed | Controller guard and `MvpFlowIntegrationTest` happy paths |
| `REVIEW_OWNER` | Yes | Denied | Denied by the same Tenant Admin guard | Denied by the same Tenant Admin guard | Direct create denial plus controller/source inspection |
| `FINAL_APPROVER` | Yes | Denied | Denied by the same Tenant Admin guard | Denied by the same Tenant Admin guard | Direct create denial plus controller/source inspection |
| `ESCALATION_OWNER` | Yes | Denied | Denied by the same Tenant Admin guard | Denied by the same Tenant Admin guard | Direct create denial plus controller/source inspection |
| `INTEGRATION_ADMIN` | Yes | Denied | Denied by the same Tenant Admin guard | Denied by the same Tenant Admin guard | Direct create denial plus controller/source inspection |
| `INTEGRATION_OPERATOR` | Yes | Denied | Denied by the same Tenant Admin guard | Denied by the same Tenant Admin guard | Direct create denial plus controller/source inspection |
| Anonymous | No | Denied by protected endpoint security | Denied | Denied | Security boundary is present; no separate product-specific anonymous assertion was added in this phase |
| Platform Owner | Not a tenant Catalog actor | No tenant mutation authority | No tenant mutation authority | No tenant mutation authority | Separate platform session/control-plane boundary |

The backend, not the frontend's disabled controls, is authoritative for mutation permission.

## Tenant Ownership

- The Product API request does not accept tenant ownership as a writable field.
- `ProductService` resolves the current tenant through `TenantContextService`.
- Product reads are filtered by tenant code.
- Product updates find the record by current tenant and product ID.
- The Product entity rejects persistence without an explicit tenant.
- The access-boundary test suite passed tenant-scoped Product reads and role-boundary checks.
- The same catalog SKU is allowed in separate tenants because the internal SKU is tenant-qualified.

No production cross-tenant ownership defect was demonstrated.

## Product and Warehouse Model

Product is tenant-wide master/reference data. It has no Warehouse relationship. Warehouse-specific quantity begins in Inventory, whose uniqueness is per Product/Warehouse pair.

Product create/update does not create Inventory rows and does not change quantities. Products are referenced by OrderItem and resolved by tenant/catalog SKU for operational flows.

## Identity Semantics

| Identifier | Meaning |
| --- | --- |
| `id` | Database persistence identifier |
| `catalogSku` | Normalized uppercase operator/integration-facing product identity |
| internal `sku` | Tenant-qualified persistence identity in the form `TENANT::CATALOGSKU` |
| response `sku` | Compatibility-facing catalog SKU value; `internalSku` separately exposes the tenant-qualified value |

Current consumers resolve the human-facing catalog SKU for inventory, orders, and scenarios. No incorrect internal-SKU usage was found. The duplicated response fields remain a documented integration-review question, not a demonstrated defect.

## Validation and Duplicate Behavior

- SKU, name, and category are required.
- SKU is trimmed, uppercased, length-limited, and restricted to the supported character pattern.
- Name and category are trimmed and length-limited.
- Invalid requests return HTTP 400.
- Same tenant plus the same normalized catalog SKU returns HTTP 409.
- Case variants cannot create a second logical SKU.
- The same catalog SKU in different tenants is allowed.
- A duplicate SKU inside one CSV returns a failed row result.

## Legacy Orphan Adoption

An orphan is a Product row with no tenant. Adoption is possible only through an authenticated tenant-scoped Product mutation. The service matches a tenant-qualified internal SKU or a catalog SKU among tenant-null legacy rows, assigns the current tenant, normalizes the identity, and updates the supplied product fields.

The `ProductionHardeningIntegrationTest` suite directly passed:

- catalog ownership migration by tenant-qualified internal SKU;
- adoption of an orphan with a matching internal SKU;
- adoption of an orphan with a matching catalog SKU.

The path does not accept tenant ownership from the request body and does not search already tenant-owned Products as orphan candidates. No cross-tenant takeover path was demonstrated. A dedicated two-tenant competition attack assertion remains useful future proof, but no production misalignment was found.

## Update and Immutability

PUT changes only catalog SKU, name, and category. Tenant, database ID, and warehouse ownership are not writable Product fields. Updating another tenant's Product is not reachable through the tenant-qualified lookup path.

There is no supported Product delete, archive, or deactivate operation. This remains a known scope limitation and was not changed.

## Operational Side-Effect Boundary

Product create and update do not directly mutate:

- Inventory quantities;
- Orders;
- Fulfillment;
- Dispatch records;
- live Alerts;
- live Recommendations;
- Scenario history.

They do record catalog business/audit evidence and publish a generic after-commit `INVENTORY_UPDATE` refresh event from source `product-catalog`. The asynchronous listener processes operational dispatch after commit. This refresh mechanism does not itself create operational alert or recommendation records.

## Test Results

Focused commands executed:

```powershell
cd backend
cmd /c "mvnw.cmd -Dtest=MvpFlowIntegrationTest,PlatformTenantAccessBoundaryIntegrationTest test"
cmd /c "mvnw.cmd -Dtest=ProductionHardeningIntegrationTest test"
```

Results:

- `MvpFlowIntegrationTest`: 80/80 passed.
- `PlatformTenantAccessBoundaryIntegrationTest`: 30/30 passed.
- `ProductionHardeningIntegrationTest`: 24/24 passed.
- Combined focused result: 134 tests, 0 failures, 0 errors, 0 skipped.

Relevant coverage includes Product create/update/import, duplicate behavior, tenant-scoped reads, role mutation denial, orphan migration/adoption, and Product use in operational flows.

## Defects and Limitations

- Production defects found: none.
- Test/fixture defects found: none.
- Dedicated negative assertions for anonymous Product mutation and every non-admin role across both PUT and CSV import are not separately enumerated; the shared backend guard and direct create-denial coverage are present.
- A dedicated two-tenant orphan-competition attack test would improve evidence completeness.
- Product deletion/deactivation, pricing, external product IDs, product versioning, and optimistic locking are outside the current supported scope.
- CSV row-level outcomes are returned individually, but transaction-level final-flush failure behavior should remain an explicit verification concern.

## Phase Result

No Critical or High Catalog production blocker was identified. The established Product authority, tenant ownership, identity, normalization, duplicate, orphan-adoption, update, and side-effect contracts are consistent with the current implementation.

**Phase 1 status: PASS with documented non-blocking evidence gaps.**

Catalog Phase 2 may proceed only as a separate authorized phase. It was not started here.

