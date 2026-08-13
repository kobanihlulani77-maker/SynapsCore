# Company Data Mapping Record

Use this template to document Company 1 Phase 6 field mapping before data onboarding. This is documentation, not a runtime mapping engine. Use synthetic examples only.

## Mapping Metadata

| Field | Value |
| --- | --- |
| Company | Company 1 |
| Tenant code |  |
| Data domain | Catalog / Inventory / Orders |
| Source system |  |
| Source-of-truth owner |  |
| Mapping prepared by |  |
| Mapping reviewed by |  |
| Date |  |
| Approved for test import |  |
| Approved for bounded live pilot data |  |

## Field Mapping

| Source field | Target field | Type | Required | Transformation | Validation | Synthetic example | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |

## Identifier Mapping

| Source identifier | SynapseCore identifier | Uniqueness scope | Tenant scope | Preservation rule |
| --- | --- | --- | --- | --- |
| Product SKU | Product `catalogSku` / API `sku` | Tenant visible SKU | Product tenant | Preserve source SKU unless approved normalization is required. |
| Warehouse/location | `Warehouse.code` | Tenant + code | Warehouse tenant | Map source location to one approved warehouse code. |
| Order id | `CustomerOrder.externalOrderId` | Tenant + external order id | Order tenant | Preserve source order id where possible. |
| Source system | `IntegrationConnector.sourceSystem` | Tenant + source + type | Connector tenant | Use approved Phase 5 connector source. |

## Normalization Rules

| Area | Rule | Approved? |
| --- | --- | --- |
| SKU case |  |
| SKU trimming |  |
| Location naming |  |
| Duplicate source identifiers |  |
| Unsupported fields excluded |  |
| Sensitive fields excluded |  |

## Rejection Rules

| Condition | Decision | Owner |
| --- | --- | --- |
| Missing required field |  |  |
| Unknown SKU |  |  |
| Unknown warehouse/location |  |  |
| Duplicate SKU |  |  |
| Duplicate external order id |  |  |
| Invalid quantity |  |  |
| Unsupported status |  |  |
| Sensitive extra field |  |  |

## Approval

| Field | Value |
| --- | --- |
| Business owner approval |  |
| Technical owner approval |  |
| SynapseCore operator approval |  |
| Ready for sample/test import |  |
| Ready for bounded live pilot data |  |
