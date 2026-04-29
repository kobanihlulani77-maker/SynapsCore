# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> @realtime dashboard summary updates live without a browser refresh
- Location: tests\prod-proof.spec.mjs:538:1

# Error details

```
Error: Expected the dashboard low-stock summary to change through the live websocket path.

expect(received).toBeLessThan(expected)

Expected: < 0
Received:   0

Call Log:
- Timeout 30000ms exceeded while waiting on the predicate
```

# Page snapshot

```yaml
- generic [ref=e3]:
  - complementary [ref=e4]:
    - button "S SynapseCore PILOT-TENANT Hosted Verification" [ref=e5] [cursor=pointer]:
      - generic [ref=e6]: S
      - generic [ref=e7]:
        - strong [ref=e8]: SynapseCore
        - generic [ref=e9]: PILOT-TENANT Hosted Verification
    - generic [ref=e10]:
      - generic [ref=e11]: Workspace
      - strong [ref=e12]: PILOT-TENANT Hosted Verification
      - paragraph [ref=e13]: Hosted Verification Tenant Admin | Operations Lead
    - navigation [ref=e14]:
      - generic [ref=e15]:
        - paragraph [ref=e16]: Overview
        - generic [ref=e17]:
          - button "Dashboard 14" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "14"
          - button "Alerts 4" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "4"
          - button "Recommendations 9" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "9"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 4" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "4"
          - button "Inventory 1" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "1"
          - button "Catalog 27" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "27"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 24" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "24"
      - generic [ref=e45]:
        - paragraph [ref=e46]: Control
        - generic [ref=e47]:
          - button "Scenarios 0" [ref=e48] [cursor=pointer]:
            - generic [ref=e49]: Scenarios
            - strong [ref=e50]: "0"
          - button "Scenario History 12" [ref=e51] [cursor=pointer]:
            - generic [ref=e52]: Scenario History
            - strong [ref=e53]: "12"
          - button "Approvals 0" [ref=e54] [cursor=pointer]:
            - generic [ref=e55]: Approvals
            - strong [ref=e56]: "0"
          - button "Escalations 8" [ref=e57] [cursor=pointer]:
            - generic [ref=e58]: Escalations
            - strong [ref=e59]: "8"
      - generic [ref=e60]:
        - paragraph [ref=e61]: Systems
        - generic [ref=e62]:
          - button "Integrations 8" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "8"
          - button "Replay Queue 0" [ref=e66] [cursor=pointer]:
            - generic [ref=e67]: Replay Queue
            - strong [ref=e68]: "0"
          - button "Runtime 8" [ref=e69] [cursor=pointer]:
            - generic [ref=e70]: Runtime
            - strong [ref=e71]: "8"
          - button "Audit & Events 20" [ref=e72] [cursor=pointer]:
            - generic [ref=e73]: Audit & Events
            - strong [ref=e74]: "20"
      - generic [ref=e75]:
        - paragraph [ref=e76]: Settings
        - generic [ref=e77]:
          - button "Users 4" [ref=e78] [cursor=pointer]:
            - generic [ref=e79]: Users
            - strong [ref=e80]: "4"
          - button "Company Settings 2" [ref=e81] [cursor=pointer]:
            - generic [ref=e82]: Company Settings
            - strong [ref=e83]: "2"
          - button "Profile 1" [ref=e84] [cursor=pointer]:
            - generic [ref=e85]: Profile
            - strong [ref=e86]: "1"
          - button "Platform Admin 8" [ref=e87] [cursor=pointer]:
            - generic [ref=e88]: Platform Admin
            - strong [ref=e89]: "8"
          - button "Tenant Management 3" [ref=e90] [cursor=pointer]:
            - generic [ref=e91]: Tenant Management
            - strong [ref=e92]: "3"
          - button "System Config 2" [ref=e93] [cursor=pointer]:
            - generic [ref=e94]: System Config
            - strong [ref=e95]: "2"
          - button "Releases 1" [ref=e96] [cursor=pointer]:
            - generic [ref=e97]: Releases
            - strong [ref=e98]: "1"
    - generic [ref=e99]:
      - generic [ref=e102]: Realtime Reconnecting
      - button "Profile & Session" [ref=e103] [cursor=pointer]
  - generic [ref=e104]:
    - banner [ref=e105]:
      - generic [ref=e106]:
        - paragraph [ref=e107]: CORE
        - heading "Live operational command center" [level=1] [ref=e108]
        - paragraph [ref=e109]: See what is happening now, what is at risk, and what the business needs to act on next.
      - generic [ref=e110]:
        - generic [ref=e112]:
          - generic [ref=e113]: Global search
          - textbox "Global search" [ref=e114]:
            - /placeholder: Search pages, orders, or alerts
        - generic "Page focus" [ref=e115]:
          - button "Act now" [ref=e116] [cursor=pointer]
          - button "Live state" [ref=e117] [cursor=pointer]
          - button "Trust layer" [ref=e118] [cursor=pointer]
        - generic [ref=e119]:
          - generic [ref=e120]: 2026/04/29, 22:06:32
          - generic [ref=e121]: Reconnecting
          - button "Notifications 12" [ref=e122] [cursor=pointer]
        - generic [ref=e123]:
          - button "Open alerts" [ref=e124] [cursor=pointer]
          - button "Open approvals" [ref=e125] [cursor=pointer]
          - button "Refresh" [ref=e126] [cursor=pointer]
          - button "Hosted Verification Tenant Admin" [ref=e127] [cursor=pointer]
          - button "Sign Out" [ref=e128] [cursor=pointer]
    - generic [ref=e129]:
      - main [ref=e130]:
        - generic [ref=e131]:
          - generic [ref=e132]:
            - paragraph [ref=e133]: Premium operational command center
            - heading "Operate live business pressure with calm, visible control." [level=2] [ref=e134]
            - paragraph [ref=e135]: One workspace for orders, stock, fulfillment, connectors, approvals, incidents, and access control. SynapseCore keeps the operating picture live, ranked, and actionable.
            - generic [ref=e136]:
              - article [ref=e137]:
                - generic [ref=e138]: Workspace Session
                - strong [ref=e139]: PILOT-TENANT Hosted Verification
                - paragraph [ref=e140]: Hosted Verification Tenant Admin is operating as Operations Lead.
              - article [ref=e141]:
                - generic [ref=e142]: Decision Pressure
                - strong [ref=e143]: 13 open signals
                - paragraph [ref=e144]: Alerts and recommendations are live across the tenant decision layer.
              - article [ref=e145]:
                - generic [ref=e146]: Operational Throughput
                - strong [ref=e147]: 4 recent orders
                - paragraph [ref=e148]: 14 backlog items and 10 delayed shipments are active.
          - article [ref=e150]:
            - paragraph [ref=e151]: Workspace pulse
            - heading "Attention needed" [level=3] [ref=e152]
            - paragraph [ref=e153]: Updated 2026/04/29, 22:06:16
            - generic [ref=e154]:
              - generic [ref=e155]:
                - generic [ref=e156]: Pages
                - strong [ref=e157]: "23"
              - generic [ref=e158]:
                - generic [ref=e159]: Connectors
                - strong [ref=e160]: 8/8
              - generic [ref=e161]:
                - generic [ref=e162]: Replay
                - strong [ref=e163]: "0"
              - generic [ref=e164]:
                - generic [ref=e165]: Approvals
                - strong [ref=e166]: "0"
        - generic [ref=e167]:
          - article [ref=e168]:
            - generic [ref=e169]: Orders
            - strong [ref=e170]: "4"
            - paragraph [ref=e171]: Live order activity in the current window
          - article [ref=e172]:
            - generic [ref=e173]: Risk
            - strong [ref=e174]: "0"
            - paragraph [ref=e175]: Inventory lanes under active pressure
          - article [ref=e176]:
            - generic [ref=e177]: Alerts
            - strong [ref=e178]: "4"
            - paragraph [ref=e179]: Warnings requiring ownership
          - article [ref=e180]:
            - generic [ref=e181]: Uptime posture
            - strong [ref=e182]: Accepting Traffic
            - paragraph [ref=e183]: System acceptance and readiness state
        - generic [ref=e184]:
          - article [ref=e185]:
            - generic [ref=e186]:
              - generic [ref=e187]:
                - paragraph [ref=e188]: Act now
                - heading "Urgent operational actions" [level=2] [ref=e189]
              - generic [ref=e190]: "5"
            - generic [ref=e191]:
              - button "Delivery delay risk rising in WH-NORTH Critical Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e192] [cursor=pointer]:
                - generic [ref=e193]:
                  - strong [ref=e194]: Delivery delay risk rising in WH-NORTH
                  - generic [ref=e195]: Critical
                - paragraph [ref=e196]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
              - button "Fulfillment backlog building in WH-NORTH Critical Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e197] [cursor=pointer]:
                - generic [ref=e198]:
                  - strong [ref=e199]: Fulfillment backlog building in WH-NORTH
                  - generic [ref=e200]: Critical
                - paragraph [ref=e201]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
              - button "Logistics anomaly detected in WH-NORTH Critical Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e202] [cursor=pointer]:
                - generic [ref=e203]:
                  - strong [ref=e204]: Logistics anomaly detected in WH-NORTH
                  - generic [ref=e205]: Critical
                - paragraph [ref=e206]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
              - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Critical Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate." [ref=e207] [cursor=pointer]:
                - generic [ref=e208]:
                  - strong [ref=e209]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - generic [ref=e210]: Critical
                - paragraph [ref=e211]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
              - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Critical Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate." [ref=e212] [cursor=pointer]:
                - generic [ref=e213]:
                  - strong [ref=e214]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - generic [ref=e215]: Critical
                - paragraph [ref=e216]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
          - article [ref=e217]:
            - generic [ref=e218]:
              - generic [ref=e219]:
                - paragraph [ref=e220]: Risk heat
                - heading "Pressure by location" [level=2] [ref=e221]
              - generic [ref=e222]: "1"
            - generic [ref=e224]:
              - generic [ref=e225]:
                - strong [ref=e226]: PILOT-TENANT Hosted Verification North Hub
                - generic [ref=e227]: Watch
              - paragraph [ref=e228]: WH-NORTH
              - paragraph [ref=e229]: 0 alerts | 0 low stock | 12 backlog | 0 delayed
        - article [ref=e231]:
          - generic [ref=e232]:
            - generic [ref=e233]:
              - paragraph [ref=e234]: Decision lanes
              - heading "Alerts and recommendations that need ownership" [level=2] [ref=e235]
            - generic [ref=e236]: "8"
          - generic [ref=e237]:
            - article [ref=e238]:
              - generic [ref=e239]:
                - strong [ref=e240]: Urgent alerts
                - button "Open Alerts" [ref=e241] [cursor=pointer]
              - generic [ref=e242]:
                - button [ref=e243] [cursor=pointer]:
                  - article [ref=e244]:
                    - generic [ref=e245]:
                      - strong [ref=e246]: Delivery delay risk rising in WH-NORTH
                      - generic [ref=e247]: CRITICAL
                    - paragraph [ref=e248]: Repeated logistics exceptions or stacked late shipments are forming in WH-NORTH. Backlog is 14 and delayed deliveries are 10.
                    - paragraph [ref=e249]: Tenant-wide | 2026/04/29, 16:38:51
                - button [ref=e250] [cursor=pointer]:
                  - article [ref=e251]:
                    - generic [ref=e252]:
                      - strong [ref=e253]: Fulfillment backlog building in WH-NORTH
                      - generic [ref=e254]: CRITICAL
                    - paragraph [ref=e255]: Repeated logistics exceptions or stacked late shipments are forming in WH-NORTH. Backlog is 14 and delayed deliveries are 10.
                    - paragraph [ref=e256]: Tenant-wide | 2026/04/24, 13:14:07
                - button [ref=e257] [cursor=pointer]:
                  - article [ref=e258]:
                    - generic [ref=e259]:
                      - strong [ref=e260]: Logistics anomaly detected in WH-NORTH
                      - generic [ref=e261]: CRITICAL
                    - paragraph [ref=e262]: Repeated logistics exceptions or stacked late shipments are forming in WH-NORTH. Backlog is 14 and delayed deliveries are 10.
                    - paragraph [ref=e263]: Tenant-wide | 2026/04/24, 14:50:21
                - button [ref=e264] [cursor=pointer]:
                  - article [ref=e265]:
                    - generic [ref=e266]:
                      - strong [ref=e267]: Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH
                      - generic [ref=e268]: HIGH
                    - paragraph [ref=e269]: Demand is accelerating and may exhaust current stock within 5.7 hours even before the reorder threshold is crossed.
                    - paragraph [ref=e270]: Tenant-wide | 2026/04/29, 22:06:14
            - article [ref=e271]:
              - generic [ref=e272]:
                - strong [ref=e273]: Recommendation queue
                - button "Open Recommendations" [ref=e274] [cursor=pointer]
              - generic [ref=e275]:
                - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate. Tenant-wide | 2026/04/29, 22:04:50" [ref=e276] [cursor=pointer]:
                  - generic [ref=e277]:
                    - strong [ref=e278]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                    - generic [ref=e279]: CRITICAL
                  - paragraph [ref=e280]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
                  - paragraph [ref=e281]: Tenant-wide | 2026/04/29, 22:04:50
                - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate. Tenant-wide | 2026/04/29, 21:27:04" [ref=e282] [cursor=pointer]:
                  - generic [ref=e283]:
                    - strong [ref=e284]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                    - generic [ref=e285]: CRITICAL
                  - paragraph [ref=e286]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
                  - paragraph [ref=e287]: Tenant-wide | 2026/04/29, 21:27:04
                - button "Investigate logistics anomaly in WH-NORTH CRITICAL Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now. Tenant-wide | 2026/04/29, 21:26:49" [ref=e288] [cursor=pointer]:
                  - generic [ref=e289]:
                    - strong [ref=e290]: Investigate logistics anomaly in WH-NORTH
                    - generic [ref=e291]: CRITICAL
                  - paragraph [ref=e292]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
                  - paragraph [ref=e293]: Tenant-wide | 2026/04/29, 21:26:49
                - button "Investigate logistics anomaly in WH-NORTH CRITICAL Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 13, delayed shipments are 10, and warehouse operations should investigate the blocked lane now. Tenant-wide | 2026/04/29, 21:26:27" [ref=e294] [cursor=pointer]:
                  - generic [ref=e295]:
                    - strong [ref=e296]: Investigate logistics anomaly in WH-NORTH
                    - generic [ref=e297]: CRITICAL
                  - paragraph [ref=e298]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 13, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
                  - paragraph [ref=e299]: Tenant-wide | 2026/04/29, 21:26:27
        - article [ref=e301]:
          - generic [ref=e302]:
            - generic [ref=e303]:
              - paragraph [ref=e304]: What changed recently
              - heading "Activity, approvals, and system health overview" [level=2] [ref=e305]
            - generic [ref=e306]: "14"
          - generic [ref=e307]:
            - article [ref=e308]:
              - generic [ref=e309]:
                - generic [ref=e310]:
                  - paragraph [ref=e311]: Live activity stream
                  - strong [ref=e312]: Recent activity
                - button "Open Audit" [ref=e313] [cursor=pointer]
              - generic [ref=e314]:
                - generic [ref=e315]:
                  - strong [ref=e316]: Request Rejected
                  - paragraph [ref=e317]: High incident
                  - paragraph [ref=e318]: 2026/04/29, 22:06:09
                - generic [ref=e319]:
                  - strong [ref=e320]: Request Rejected
                  - paragraph [ref=e321]: High incident
                  - paragraph [ref=e322]: 2026/04/29, 22:06:03
                - generic [ref=e323]:
                  - strong [ref=e324]: Inventory Updated
                  - paragraph [ref=e325]: Inventory baseline set for SKU-PILOT-TENANT-PROOF in WH-NORTH to on-hand 39.
                  - paragraph [ref=e326]: 2026/04/29, 22:06:13
                - generic [ref=e327]:
                  - strong [ref=e328]: Product Catalog Updated
                  - paragraph [ref=e329]: "Imported product catalog rows: created 1, updated 1, failed 1."
                  - paragraph [ref=e330]: 2026/04/29, 22:05:51
            - article [ref=e331]:
              - generic [ref=e332]:
                - strong [ref=e333]: Approval attention
                - button "Open Approvals" [ref=e334] [cursor=pointer]
              - generic [ref=e336]: No plans are waiting on review right now. Decision routing will appear here when operators start sending scenarios through governance.
            - article [ref=e337]:
              - generic [ref=e338]:
                - strong [ref=e339]: System health overview
                - button "Open Runtime" [ref=e340] [cursor=pointer]
              - generic [ref=e341]:
                - generic [ref=e342]:
                  - generic [ref=e343]:
                    - strong [ref=e344]: Runtime trust
                    - generic [ref=e345]: Up
                  - paragraph [ref=e346]: Accepting Traffic readiness with 2 pending dispatch items.
                  - paragraph [ref=e347]: Build 0.0.1 | Commit render-
                - generic [ref=e348]:
                  - generic [ref=e349]:
                    - strong [ref=e350]: Incident pressure
                    - generic [ref=e351]: 8 active
                  - paragraph [ref=e352]: 8 high-severity incidents and 0 replay items are currently affecting trust.
                  - paragraph [ref=e353]: 8/8 connectors enabled across the tenant integration surface.
      - complementary [ref=e354]:
        - article [ref=e355]:
          - generic [ref=e356]:
            - paragraph [ref=e357]: Realtime state
            - generic [ref=e358]: Reconnecting
          - strong [ref=e359]: Monitoring live operating state
          - paragraph [ref=e360]: Snapshot 2026/04/29, 22:06:39
          - generic [ref=e361]:
            - generic [ref=e362]:
              - generic [ref=e363]: Alerts
              - strong [ref=e364]: "4"
            - generic [ref=e365]:
              - generic [ref=e366]: Actions
              - strong [ref=e367]: "12"
            - generic [ref=e368]:
              - generic [ref=e369]: Replay
              - strong [ref=e370]: "0"
            - generic [ref=e371]:
              - generic [ref=e372]: Incidents
              - strong [ref=e373]: "8"
        - article [ref=e374]:
          - generic [ref=e375]:
            - paragraph [ref=e376]: Page focus
            - generic [ref=e377]: Dashboard
          - strong [ref=e378]: Live operational command center
          - paragraph [ref=e379]: Updated 2026/04/29, 22:06:16
          - generic [ref=e380]:
            - generic [ref=e381]:
              - generic [ref=e382]: Focus 1
              - strong [ref=e383]: Act now
            - generic [ref=e384]:
              - generic [ref=e385]: Focus 2
              - strong [ref=e386]: Live state
            - generic [ref=e387]:
              - generic [ref=e388]: Focus 3
              - strong [ref=e389]: Trust layer
            - generic [ref=e390]:
              - generic [ref=e391]: Group
              - strong [ref=e392]: Core
        - article [ref=e393]:
          - generic [ref=e394]:
            - paragraph [ref=e395]: Act now
            - generic [ref=e396]: 5 items
          - generic [ref=e397]:
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e398] [cursor=pointer]:
              - generic [ref=e399]: Critical
              - strong [ref=e400]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e401]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e402] [cursor=pointer]:
              - generic [ref=e403]: Critical
              - strong [ref=e404]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e405]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Logistics anomaly detected in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e406] [cursor=pointer]:
              - generic [ref=e407]: Critical
              - strong [ref=e408]: Logistics anomaly detected in WH-NORTH
              - paragraph [ref=e409]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 14, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate." [ref=e410] [cursor=pointer]:
              - generic [ref=e411]: Critical
              - strong [ref=e412]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
              - paragraph [ref=e413]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
            - button "Critical Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate." [ref=e414] [cursor=pointer]:
              - generic [ref=e415]: Critical
              - strong [ref=e416]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
              - paragraph [ref=e417]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
        - article [ref=e418]:
          - generic [ref=e419]:
            - paragraph [ref=e420]: Activity stream
            - generic [ref=e421]: "6"
          - generic [ref=e422]:
            - generic [ref=e423]:
              - strong [ref=e424]: Request Rejected
              - paragraph [ref=e425]: High incident
              - generic [ref=e426]: 2026/04/29, 22:06:09
            - generic [ref=e427]:
              - strong [ref=e428]: Request Rejected
              - paragraph [ref=e429]: High incident
              - generic [ref=e430]: 2026/04/29, 22:06:03
            - generic [ref=e431]:
              - strong [ref=e432]: Inventory Updated
              - paragraph [ref=e433]: Inventory baseline set for SKU-PILOT-TENANT-PROOF in WH-NORTH to on-hand 39.
              - generic [ref=e434]: 2026/04/29, 22:06:13
            - generic [ref=e435]:
              - strong [ref=e436]: Product Catalog Updated
              - paragraph [ref=e437]: "Imported product catalog rows: created 1, updated 1, failed 1."
              - generic [ref=e438]: 2026/04/29, 22:05:51
            - generic [ref=e439]:
              - strong [ref=e440]: Inventory Updated
              - paragraph [ref=e441]: Inventory | SKU-PILOT-TENANT-PROOF@WH-NORTH
              - generic [ref=e442]: 2026/04/29, 22:06:14
            - generic [ref=e443]:
              - strong [ref=e444]: Request Rejected
              - paragraph [ref=e445]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e446]: 2026/04/29, 22:06:09
        - article [ref=e447]:
          - generic [ref=e448]:
            - paragraph [ref=e449]: Operator
            - generic [ref=e450]: Healthy
          - strong [ref=e451]: Hosted Verification Tenant Admin
          - paragraph [ref=e452]: PILOT-TENANT Hosted Verification | Operations Lead
          - paragraph [ref=e453]: Roles Escalation Owner, Integration Admin, Integration Operator, Review Owner, Tenant Admin
          - paragraph [ref=e454]: Warehouse scope Tenant-wide
```

# Test source

```ts
  471 |   for (const [route, title] of appPages) {
  472 |     await navigateWithinApp(page, route)
  473 |     await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  474 |     await expect(page.locator('.workspace-topbar')).toBeVisible()
  475 |     await expectNoFatalUiErrors(page)
  476 |   }
  477 | 
  478 |   await signOutViaUi(page)
  479 | })
  480 | 
  481 | test('product catalog onboarding works through tenant-scoped API and browser surface', async ({ page }) => {
  482 |   const api = await createApiContext(users.operationsLead)
  483 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  484 |   const primarySku = `SKU-UI-${suffix}`
  485 |   const importSku = `SKU-IMP-${suffix}`
  486 | 
  487 |   try {
  488 |     const createdProduct = await readJson(await api.post('/api/products', {
  489 |       data: {
  490 |         sku: primarySku,
  491 |         name: `UI Catalog ${suffix}`,
  492 |         category: 'Verification',
  493 |       },
  494 |     }))
  495 |     expect(createdProduct.sku).toBe(primarySku)
  496 |     expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)
  497 | 
  498 |     const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
  499 |       data: {
  500 |         sku: primarySku,
  501 |         name: `UI Catalog ${suffix} Updated`,
  502 |         category: 'Verification',
  503 |       },
  504 |     }))
  505 |     expect(updatedProduct.name).toContain('Updated')
  506 | 
  507 |     const importResult = await readJson(await api.post('/api/products/import', {
  508 |       multipart: {
  509 |         file: {
  510 |           name: 'products.csv',
  511 |           mimeType: 'text/csv',
  512 |           buffer: Buffer.from(
  513 |             `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
  514 |             'utf8',
  515 |           ),
  516 |         },
  517 |       },
  518 |     }))
  519 |     expect(importResult.created).toBe(1)
  520 |     expect(importResult.updated).toBe(1)
  521 |     expect(importResult.failed).toBe(1)
  522 | 
  523 |     const products = await readJson(await api.get('/api/products'))
  524 |     expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  525 |     expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  526 | 
  527 |     await loginViaUi(page, users.operationsLead)
  528 |     await navigateWithinApp(page, '/catalog')
  529 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
  530 |     await expect(page.getByText(primarySku).first()).toBeVisible()
  531 |     await expect(page.getByText(importSku).first()).toBeVisible()
  532 |     await expectNoFatalUiErrors(page)
  533 |   } finally {
  534 |     await api.dispose()
  535 |   }
  536 | })
  537 | 
  538 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  539 |   await loginViaUi(page, users.operationsLead)
  540 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  541 |   await expect(page.getByText('Realtime state')).toBeVisible()
  542 | 
  543 |   const api = await createApiContext(users.operationsLead)
  544 |   let candidate = null
  545 |   let revertQuantity = null
  546 |   let revertThreshold = null
  547 | 
  548 |   try {
  549 |     const inventory = await readJson(await api.get('/api/inventory'))
  550 |     candidate = inventory.find((item) => item.productSku === proofProductSku && item.warehouseCode === 'WH-NORTH')
  551 |       || inventory.find((item) => Number.isFinite(item.quantityAvailable) && Number.isFinite(item.reorderThreshold))
  552 |     expect(candidate).toBeTruthy()
  553 | 
  554 |     revertQuantity = candidate.quantityAvailable
  555 |     revertThreshold = candidate.reorderThreshold
  556 | 
  557 |     const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
  558 |     const threshold = Number.isFinite(candidate.reorderThreshold) ? candidate.reorderThreshold : 5
  559 |     const forceLowQuantity = Math.max(0, threshold - 1)
  560 |     const safeQuantity = threshold + 5
  561 | 
  562 |     await readJson(await api.post('/api/inventory/update', {
  563 |       data: {
  564 |         productSku: candidate.productSku,
  565 |         warehouseCode: candidate.warehouseCode,
  566 |         quantityAvailable: candidate.lowStock ? safeQuantity : forceLowQuantity,
  567 |         reorderThreshold: threshold,
  568 |       },
  569 |     }))
  570 | 
> 571 |     await expect.poll(async () => summaryCardValue(page, 'Risk'), {
      |     ^ Error: Expected the dashboard low-stock summary to change through the live websocket path.
  572 |       timeout: 30_000,
  573 |       message: 'Expected the dashboard low-stock summary to change through the live websocket path.',
  574 |     })[candidate.lowStock ? 'toBeLessThan' : 'toBeGreaterThan'](beforeRisk)
  575 |   } finally {
  576 |     if (candidate && revertQuantity != null && revertThreshold != null) {
  577 |       await readJson(await api.post('/api/inventory/update', {
  578 |         data: {
  579 |           productSku: candidate.productSku,
  580 |           warehouseCode: candidate.warehouseCode,
  581 |           quantityAvailable: revertQuantity,
  582 |           reorderThreshold: revertThreshold,
  583 |         },
  584 |       }))
  585 |     }
  586 |     await api.dispose()
  587 |   }
  588 | })
  589 | 
  590 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  591 |   const replayFixture = await createReplayFixture()
  592 | 
  593 |   try {
  594 |     await loginViaUi(page, users.integrationLead)
  595 |     await navigateWithinApp(page, '/replay-queue')
  596 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  597 | 
  598 |     const initialReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  599 |     if (initialReplayOutcome.state === 'queued') {
  600 |       const replayQueueRecord = page.locator('.signal-list-item').filter({ hasText: replayFixture.externalOrderId }).first()
  601 |       if (await replayQueueRecord.isVisible().catch(() => false)) {
  602 |         await replayQueueRecord.click()
  603 |       }
  604 | 
  605 |       const replayButton = page.getByRole('button', { name: 'Replay Into Live Flow' }).first()
  606 |       if (await replayButton.isVisible().catch(() => false) && await replayButton.isEnabled().catch(() => false)) {
  607 |         try {
  608 |           await replayButton.click({ timeout: 10_000 })
  609 |         } catch (error) {
  610 |           const message = error instanceof Error ? error.message : String(error)
  611 |           if (!/detached|timeout/i.test(message)) {
  612 |             throw error
  613 |           }
  614 |         }
  615 |       }
  616 |     }
  617 | 
  618 |     await expect.poll(async () => (await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)).state, {
  619 |       timeout: 60_000,
  620 |       message: `Expected ${replayFixture.externalOrderId} to reach a replayed state through manual or automated recovery.`,
  621 |     }).toBe('replayed')
  622 | 
  623 |   await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  624 |   } finally {
  625 |     await replayFixture.api.dispose()
  626 |   }
  627 | 
  628 |   await signOutViaUi(page)
  629 | 
  630 |   const scenarioFixture = await createScenarioFixture()
  631 | 
  632 |   try {
  633 |     await loginViaUi(page, users.operationsLead)
  634 |     await navigateWithinApp(page, '/scenario-history')
  635 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  636 | 
  637 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  638 |       hasText: scenarioFixture.title,
  639 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  640 |     }).first()
  641 |     await expect(scenarioApprovalConsole).toBeVisible()
  642 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  643 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  644 | 
  645 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  646 |       hasText: scenarioFixture.title,
  647 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  648 |     }).first()
  649 |     await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  650 |     await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  651 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  652 |   } finally {
  653 |     await scenarioFixture.api.dispose()
  654 |   }
  655 | 
  656 |   await signOutViaUi(page)
  657 | 
  658 |   await loginViaUi(page, users.operationsPlanner)
  659 |   await navigateWithinApp(page, '/users')
  660 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  661 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  662 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  663 | })
  664 | 
  665 | test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }) => {
  666 |   const api = await createApiContext(users.operationsLead)
  667 |   let restoreAlertCoverage = async () => {}
  668 | 
  669 |   try {
  670 |     const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
  671 |     restoreAlertCoverage = alertCoverage.restore
```