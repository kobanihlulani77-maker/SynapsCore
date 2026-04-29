# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> @realtime dashboard summary updates live without a browser refresh
- Location: tests\prod-proof.spec.mjs:574:1

# Error details

```
Error: Expected the dashboard low-stock summary to increase through the live websocket path for SKU-RT-4849B312.

expect(received).toBe(expected) // Object.is equality

Expected: 1
Received: 0

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
          - button "Dashboard 16" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "16"
          - button "Alerts 4" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "4"
          - button "Recommendations 14" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "14"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 6" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "6"
          - button "Inventory 4" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "4"
          - button "Catalog 36" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "36"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 26" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "26"
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
          - button "Integrations 10" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "10"
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
          - button "System Config 4" [ref=e93] [cursor=pointer]:
            - generic [ref=e94]: System Config
            - strong [ref=e95]: "4"
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
          - generic [ref=e120]: 2026/04/29, 23:03:20
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
                - strong [ref=e143]: 18 open signals
                - paragraph [ref=e144]: Alerts and recommendations are live across the tenant decision layer.
              - article [ref=e145]:
                - generic [ref=e146]: Operational Throughput
                - strong [ref=e147]: 6 recent orders
                - paragraph [ref=e148]: 16 backlog items and 10 delayed shipments are active.
          - article [ref=e150]:
            - paragraph [ref=e151]: Workspace pulse
            - heading "Attention needed" [level=3] [ref=e152]
            - paragraph [ref=e153]: Updated 2026/04/29, 23:02:57
            - generic [ref=e154]:
              - generic [ref=e155]:
                - generic [ref=e156]: Pages
                - strong [ref=e157]: "23"
              - generic [ref=e158]:
                - generic [ref=e159]: Connectors
                - strong [ref=e160]: 10/10
              - generic [ref=e161]:
                - generic [ref=e162]: Replay
                - strong [ref=e163]: "0"
              - generic [ref=e164]:
                - generic [ref=e165]: Approvals
                - strong [ref=e166]: "0"
        - generic [ref=e167]:
          - article [ref=e168]:
            - generic [ref=e169]: Orders
            - strong [ref=e170]: "6"
            - paragraph [ref=e171]: Live order activity in the current window
          - article [ref=e172]:
            - generic [ref=e173]: Risk
            - strong [ref=e174]: "1"
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
              - button "Low stock detected for SKU SKU-RT-4849B312 in WH-NORTH Critical Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e192] [cursor=pointer]:
                - generic [ref=e193]:
                  - strong [ref=e194]: Low stock detected for SKU SKU-RT-4849B312 in WH-NORTH
                  - generic [ref=e195]: Critical
                - paragraph [ref=e196]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
              - button "Delivery delay risk rising in WH-NORTH Critical Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e197] [cursor=pointer]:
                - generic [ref=e198]:
                  - strong [ref=e199]: Delivery delay risk rising in WH-NORTH
                  - generic [ref=e200]: Critical
                - paragraph [ref=e201]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
              - button "Fulfillment backlog building in WH-NORTH Critical Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e202] [cursor=pointer]:
                - generic [ref=e203]:
                  - strong [ref=e204]: Fulfillment backlog building in WH-NORTH
                  - generic [ref=e205]: Critical
                - paragraph [ref=e206]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
              - button "Urgent reorder for SKU SKU-RT-4849B312 at WH-NORTH Critical Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e207] [cursor=pointer]:
                - generic [ref=e208]:
                  - strong [ref=e209]: Urgent reorder for SKU SKU-RT-4849B312 at WH-NORTH
                  - generic [ref=e210]: Critical
                - paragraph [ref=e211]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
              - button "Urgent reorder for SKU SKU-RT-681295A4 at WH-NORTH Critical Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e212] [cursor=pointer]:
                - generic [ref=e213]:
                  - strong [ref=e214]: Urgent reorder for SKU SKU-RT-681295A4 at WH-NORTH
                  - generic [ref=e215]: Critical
                - paragraph [ref=e216]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
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
              - paragraph [ref=e229]: 0 alerts | 1 low stock | 12 backlog | 0 delayed
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
                      - strong [ref=e246]: Low stock detected for SKU SKU-RT-4849B312 in WH-NORTH
                      - generic [ref=e247]: CRITICAL
                    - paragraph [ref=e248]: Available stock is below the configured threshold and needs replenishment attention.
                    - paragraph [ref=e249]: Tenant-wide | 2026/04/29, 23:03:00
                - button [ref=e250] [cursor=pointer]:
                  - article [ref=e251]:
                    - generic [ref=e252]:
                      - strong [ref=e253]: Delivery delay risk rising in WH-NORTH
                      - generic [ref=e254]: CRITICAL
                    - paragraph [ref=e255]: Repeated logistics exceptions or stacked late shipments are forming in WH-NORTH. Backlog is 16 and delayed deliveries are 10.
                    - paragraph [ref=e256]: Tenant-wide | 2026/04/29, 16:38:51
                - button [ref=e257] [cursor=pointer]:
                  - article [ref=e258]:
                    - generic [ref=e259]:
                      - strong [ref=e260]: Fulfillment backlog building in WH-NORTH
                      - generic [ref=e261]: CRITICAL
                    - paragraph [ref=e262]: Repeated logistics exceptions or stacked late shipments are forming in WH-NORTH. Backlog is 16 and delayed deliveries are 10.
                    - paragraph [ref=e263]: Tenant-wide | 2026/04/24, 13:14:07
                - button [ref=e264] [cursor=pointer]:
                  - article [ref=e265]:
                    - generic [ref=e266]:
                      - strong [ref=e267]: Logistics anomaly detected in WH-NORTH
                      - generic [ref=e268]: CRITICAL
                    - paragraph [ref=e269]: Repeated logistics exceptions or stacked late shipments are forming in WH-NORTH. Backlog is 16 and delayed deliveries are 10.
                    - paragraph [ref=e270]: Tenant-wide | 2026/04/24, 14:50:21
            - article [ref=e271]:
              - generic [ref=e272]:
                - strong [ref=e273]: Recommendation queue
                - button "Open Recommendations" [ref=e274] [cursor=pointer]
              - generic [ref=e275]:
                - button "Urgent reorder for SKU SKU-RT-4849B312 at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10. Tenant-wide | 2026/04/29, 23:02:59" [ref=e276] [cursor=pointer]:
                  - generic [ref=e277]:
                    - strong [ref=e278]: Urgent reorder for SKU SKU-RT-4849B312 at WH-NORTH
                    - generic [ref=e279]: CRITICAL
                  - paragraph [ref=e280]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
                  - paragraph [ref=e281]: Tenant-wide | 2026/04/29, 23:02:59
                - button "Urgent reorder for SKU SKU-RT-681295A4 at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10. Tenant-wide | 2026/04/29, 22:55:14" [ref=e282] [cursor=pointer]:
                  - generic [ref=e283]:
                    - strong [ref=e284]: Urgent reorder for SKU SKU-RT-681295A4 at WH-NORTH
                    - generic [ref=e285]: CRITICAL
                  - paragraph [ref=e286]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
                  - paragraph [ref=e287]: Tenant-wide | 2026/04/29, 22:55:14
                - button "Urgent reorder for SKU SKU-RT-934E719D at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10. Tenant-wide | 2026/04/29, 22:48:20" [ref=e288] [cursor=pointer]:
                  - generic [ref=e289]:
                    - strong [ref=e290]: Urgent reorder for SKU SKU-RT-934E719D at WH-NORTH
                    - generic [ref=e291]: CRITICAL
                  - paragraph [ref=e292]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
                  - paragraph [ref=e293]: Tenant-wide | 2026/04/29, 22:48:20
                - button "Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH CRITICAL Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate. Tenant-wide | 2026/04/29, 22:04:50" [ref=e294] [cursor=pointer]:
                  - generic [ref=e295]:
                    - strong [ref=e296]: Urgent reorder for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                    - generic [ref=e297]: CRITICAL
                  - paragraph [ref=e298]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.7 hours at the current demand rate.
                  - paragraph [ref=e299]: Tenant-wide | 2026/04/29, 22:04:50
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
                  - paragraph [ref=e318]: 2026/04/29, 23:03:03
                - generic [ref=e319]:
                  - strong [ref=e320]: Request Rejected
                  - paragraph [ref=e321]: High incident
                  - paragraph [ref=e322]: 2026/04/29, 23:02:53
                - generic [ref=e323]:
                  - strong [ref=e324]: Low Stock Detected
                  - paragraph [ref=e325]: SKU-RT-4849B312 fell below threshold in WH-NORTH
                  - paragraph [ref=e326]: 2026/04/29, 23:03:00
                - generic [ref=e327]:
                  - strong [ref=e328]: Recommendation Generated
                  - paragraph [ref=e329]: Generated CRITICAL recommendation for SKU-RT-4849B312 in WH-NORTH
                  - paragraph [ref=e330]: 2026/04/29, 23:02:59
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
                  - paragraph [ref=e346]: Accepting Traffic readiness with 4 pending dispatch items.
                  - paragraph [ref=e347]: Build 0.0.1 | Commit render-
                - generic [ref=e348]:
                  - generic [ref=e349]:
                    - strong [ref=e350]: Incident pressure
                    - generic [ref=e351]: 8 active
                  - paragraph [ref=e352]: 7 high-severity incidents and 0 replay items are currently affecting trust.
                  - paragraph [ref=e353]: 10/10 connectors enabled across the tenant integration surface.
      - complementary [ref=e354]:
        - article [ref=e355]:
          - generic [ref=e356]:
            - paragraph [ref=e357]: Realtime state
            - generic [ref=e358]: Reconnecting
          - strong [ref=e359]: Monitoring live operating state
          - paragraph [ref=e360]: Snapshot 2026/04/29, 23:03:34
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
          - paragraph [ref=e379]: Updated 2026/04/29, 23:02:57
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
            - button "Critical Low stock detected for SKU SKU-RT-4849B312 in WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e398] [cursor=pointer]:
              - generic [ref=e399]: Critical
              - strong [ref=e400]: Low stock detected for SKU SKU-RT-4849B312 in WH-NORTH
              - paragraph [ref=e401]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e402] [cursor=pointer]:
              - generic [ref=e403]: Critical
              - strong [ref=e404]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e405]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e406] [cursor=pointer]:
              - generic [ref=e407]: Critical
              - strong [ref=e408]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e409]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 16, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-RT-4849B312 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e410] [cursor=pointer]:
              - generic [ref=e411]: Critical
              - strong [ref=e412]: Urgent reorder for SKU SKU-RT-4849B312 at WH-NORTH
              - paragraph [ref=e413]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Urgent reorder for SKU SKU-RT-681295A4 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e414] [cursor=pointer]:
              - generic [ref=e415]: Critical
              - strong [ref=e416]: Urgent reorder for SKU SKU-RT-681295A4 at WH-NORTH
              - paragraph [ref=e417]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
        - article [ref=e418]:
          - generic [ref=e419]:
            - paragraph [ref=e420]: Activity stream
            - generic [ref=e421]: "6"
          - generic [ref=e422]:
            - generic [ref=e423]:
              - strong [ref=e424]: Request Rejected
              - paragraph [ref=e425]: High incident
              - generic [ref=e426]: 2026/04/29, 23:03:03
            - generic [ref=e427]:
              - strong [ref=e428]: Request Rejected
              - paragraph [ref=e429]: High incident
              - generic [ref=e430]: 2026/04/29, 23:02:53
            - generic [ref=e431]:
              - strong [ref=e432]: Low Stock Detected
              - paragraph [ref=e433]: SKU-RT-4849B312 fell below threshold in WH-NORTH
              - generic [ref=e434]: 2026/04/29, 23:03:00
            - generic [ref=e435]:
              - strong [ref=e436]: Recommendation Generated
              - paragraph [ref=e437]: Generated CRITICAL recommendation for SKU-RT-4849B312 in WH-NORTH
              - generic [ref=e438]: 2026/04/29, 23:02:59
            - generic [ref=e439]:
              - strong [ref=e440]: Request Rejected
              - paragraph [ref=e441]: ApiRequest | /api/dashboard/snapshot
              - generic [ref=e442]: 2026/04/29, 23:03:03
            - generic [ref=e443]:
              - strong [ref=e444]: Inventory Updated
              - paragraph [ref=e445]: Inventory | SKU-RT-4849B312@WH-NORTH
              - generic [ref=e446]: 2026/04/29, 23:03:00
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
  494 |   await fillSignInForm(signInCard, users.operationsLead, 'wrong-code')
  495 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  496 |   await expect(signInCard.getByRole('button', { name: 'Enter Platform' })).toBeEnabled({ timeout: 60_000 })
  497 |   await expect(signInCard.getByText('Invalid operator credentials.')).toBeVisible({ timeout: 15_000 })
  498 | 
  499 |   await waitForSignInReady(signInCard)
  500 |   await fillSignInForm(signInCard, users.operationsLead, users.operationsLead.password)
  501 |   await signInCard.getByRole('button', { name: 'Enter Platform' }).click()
  502 |   await expect(page).toHaveURL(/\/dashboard$/)
  503 |   await page.reload()
  504 |   await expect(page).toHaveURL(/\/dashboard$/)
  505 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  506 | 
  507 |   for (const [route, title] of appPages) {
  508 |     await navigateWithinApp(page, route)
  509 |     await expect(page.getByRole('heading', { level: 1, name: title })).toBeVisible()
  510 |     await expect(page.locator('.workspace-topbar')).toBeVisible()
  511 |     await expectNoFatalUiErrors(page)
  512 |   }
  513 | 
  514 |   await signOutViaUi(page)
  515 | })
  516 | 
  517 | test('product catalog onboarding works through tenant-scoped API and browser surface', async ({ page }) => {
  518 |   const api = await createApiContext(users.operationsLead)
  519 |   const suffix = randomUUID().slice(0, 8).toUpperCase()
  520 |   const primarySku = `SKU-UI-${suffix}`
  521 |   const importSku = `SKU-IMP-${suffix}`
  522 | 
  523 |   try {
  524 |     const createdProduct = await readJson(await api.post('/api/products', {
  525 |       data: {
  526 |         sku: primarySku,
  527 |         name: `UI Catalog ${suffix}`,
  528 |         category: 'Verification',
  529 |       },
  530 |     }))
  531 |     expect(createdProduct.sku).toBe(primarySku)
  532 |     expect(createdProduct.tenantCode).toBe(users.operationsLead.tenantCode)
  533 | 
  534 |     const updatedProduct = await readJson(await api.put(`/api/products/${createdProduct.id}`, {
  535 |       data: {
  536 |         sku: primarySku,
  537 |         name: `UI Catalog ${suffix} Updated`,
  538 |         category: 'Verification',
  539 |       },
  540 |     }))
  541 |     expect(updatedProduct.name).toContain('Updated')
  542 | 
  543 |     const importResult = await readJson(await api.post('/api/products/import', {
  544 |       multipart: {
  545 |         file: {
  546 |           name: 'products.csv',
  547 |           mimeType: 'text/csv',
  548 |           buffer: Buffer.from(
  549 |             `sku,name,category\n${importSku},Imported Product ${suffix},Verification\n${primarySku},Imported Update ${suffix},Verification\n${importSku},Duplicate Product ${suffix},Verification\n`,
  550 |             'utf8',
  551 |           ),
  552 |         },
  553 |       },
  554 |     }))
  555 |     expect(importResult.created).toBe(1)
  556 |     expect(importResult.updated).toBe(1)
  557 |     expect(importResult.failed).toBe(1)
  558 | 
  559 |     const products = await readJson(await api.get('/api/products'))
  560 |     expect(products.some((product) => product.sku === primarySku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  561 |     expect(products.some((product) => product.sku === importSku && product.tenantCode === users.operationsLead.tenantCode)).toBeTruthy()
  562 | 
  563 |     await loginViaUi(page, users.operationsLead)
  564 |     await navigateWithinApp(page, '/catalog')
  565 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant product catalog' })).toBeVisible()
  566 |     await expect(page.getByText(primarySku).first()).toBeVisible()
  567 |     await expect(page.getByText(importSku).first()).toBeVisible()
  568 |     await expectNoFatalUiErrors(page)
  569 |   } finally {
  570 |     await api.dispose()
  571 |   }
  572 | })
  573 | 
  574 | test('@realtime dashboard summary updates live without a browser refresh', async ({ page }) => {
  575 |   const api = await createApiContext(users.operationsLead)
  576 |   const realtimeFixture = await createRealtimeInventoryFixture(api)
  577 | 
  578 |   await loginViaUi(page, users.operationsLead)
  579 |   await expect(page.getByRole('heading', { level: 1, name: 'Live operational command center' })).toBeVisible()
  580 |   await expect(page.getByText('Realtime state')).toBeVisible()
  581 | 
  582 |   try {
  583 |     const beforeRisk = await waitForNumericSummaryCard(page, 'Risk')
  584 | 
  585 |     await readJson(await api.post('/api/inventory/update', {
  586 |       data: {
  587 |         productSku: realtimeFixture.productSku,
  588 |         warehouseCode: realtimeFixture.warehouseCode,
  589 |         quantityAvailable: realtimeFixture.lowQuantity,
  590 |         reorderThreshold: realtimeFixture.reorderThreshold,
  591 |       },
  592 |     }))
  593 | 
> 594 |     await expect.poll(async () => summaryCardValue(page, 'Risk'), {
      |     ^ Error: Expected the dashboard low-stock summary to increase through the live websocket path for SKU-RT-4849B312.
  595 |       timeout: 30_000,
  596 |       message: `Expected the dashboard low-stock summary to increase through the live websocket path for ${realtimeFixture.productSku}.`,
  597 |     }).toBe(beforeRisk + 1)
  598 |   } finally {
  599 |     await readJson(await api.post('/api/inventory/update', {
  600 |       data: {
  601 |         productSku: realtimeFixture.productSku,
  602 |         warehouseCode: realtimeFixture.warehouseCode,
  603 |         quantityAvailable: realtimeFixture.safeQuantity,
  604 |         reorderThreshold: realtimeFixture.reorderThreshold,
  605 |       },
  606 |     }))
  607 |     await api.dispose()
  608 |   }
  609 | })
  610 | 
  611 | test('replay recovery, scenario approval, execution, and browser role gating work through the UI', async ({ page }) => {
  612 |   const replayFixture = await createReplayFixture()
  613 | 
  614 |   try {
  615 |     await loginViaUi(page, users.integrationLead)
  616 |     await navigateWithinApp(page, '/replay-queue')
  617 |     await expect(page.getByRole('heading', { level: 1, name: 'Failed inbound recovery' })).toBeVisible()
  618 | 
  619 |     const initialReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  620 |     if (initialReplayOutcome.state === 'queued') {
  621 |       const replayQueueRecord = page.locator('.signal-list-item').filter({ hasText: replayFixture.externalOrderId }).first()
  622 |       await expect(replayQueueRecord).toBeVisible()
  623 |       await replayQueueRecord.click()
  624 | 
  625 |       const replayDetail = page.locator('.section-card').filter({ hasText: 'Recovery detail' }).first()
  626 |       await expect(replayDetail.getByText(replayFixture.externalOrderId).first()).toBeVisible()
  627 | 
  628 |       const replayButton = replayDetail.getByRole('button', { name: 'Replay Into Live Flow' })
  629 |       await expect(replayButton).toBeVisible()
  630 |       await expect(replayButton).toBeEnabled()
  631 | 
  632 |       const replayResponsePromise = page.waitForResponse((response) => (
  633 |         response.request().method() === 'POST'
  634 |           && /\/api\/integrations\/orders\/replay\/\d+$/i.test(response.url())
  635 |       ), { timeout: 20_000 })
  636 | 
  637 |       let replayResponse = null
  638 |       try {
  639 |         await replayButton.scrollIntoViewIfNeeded()
  640 |         ;[replayResponse] = await Promise.all([
  641 |           replayResponsePromise,
  642 |           replayButton.click(),
  643 |         ])
  644 |       } catch (error) {
  645 |         const currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  646 |         if (currentReplayOutcome.state !== 'replayed') {
  647 |           throw error
  648 |         }
  649 |       }
  650 | 
  651 |       if (replayResponse) {
  652 |         const replayPayload = await replayResponse.json().catch(() => null)
  653 |         if (!replayResponse.ok()) {
  654 |           throw new Error(
  655 |             replayPayload?.message
  656 |               || `Replay request failed with status ${replayResponse.status()} for ${replayFixture.externalOrderId}.`,
  657 |           )
  658 |         }
  659 |         await expect(page.locator('.success-text').filter({
  660 |           hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.`,
  661 |         }).first()).toBeVisible()
  662 |       }
  663 |     }
  664 | 
  665 |     await expect.poll(async () => {
  666 |       const replayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  667 |       return replayOutcome.state === 'queued' ? `${replayOutcome.state}:${replayOutcome.status}` : replayOutcome.state
  668 |     }, {
  669 |       timeout: 60_000,
  670 |       message: `Expected ${replayFixture.externalOrderId} to reach a replayed state through manual or automated recovery.`,
  671 |     }).toBe('replayed')
  672 | 
  673 |   await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  674 |   } finally {
  675 |     await replayFixture.api.dispose()
  676 |   }
  677 | 
  678 |   await signOutViaUi(page)
  679 | 
  680 |   const scenarioFixture = await createScenarioFixture()
  681 | 
  682 |   try {
  683 |     await loginViaUi(page, users.operationsLead)
  684 |     await navigateWithinApp(page, '/scenario-history')
  685 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  686 | 
  687 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  688 |       hasText: scenarioFixture.title,
  689 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  690 |     }).first()
  691 |     await expect(scenarioApprovalConsole).toBeVisible()
  692 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  693 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  694 | 
```