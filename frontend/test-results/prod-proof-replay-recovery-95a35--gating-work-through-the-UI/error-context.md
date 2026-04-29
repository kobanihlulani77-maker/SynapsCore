# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> replay recovery, scenario approval, execution, and browser role gating work through the UI
- Location: tests\prod-proof.spec.mjs:663:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: locator('.stack-card').filter({ hasText: 'UI Scenario 655D9FE7' }).filter({ has: getByRole('button', { name: 'Approve Plan' }) }).first()
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "toBeVisible" with timeout 20000ms
  - waiting for locator('.stack-card').filter({ hasText: 'UI Scenario 655D9FE7' }).filter({ has: getByRole('button', { name: 'Approve Plan' }) }).first()

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
          - button "Dashboard 22" [ref=e18] [cursor=pointer]:
            - generic [ref=e19]: Dashboard
            - strong [ref=e20]: "22"
          - button "Alerts 4" [ref=e21] [cursor=pointer]:
            - generic [ref=e22]: Alerts
            - strong [ref=e23]: "4"
          - button "Recommendations 38" [ref=e24] [cursor=pointer]:
            - generic [ref=e25]: Recommendations
            - strong [ref=e26]: "38"
      - generic [ref=e27]:
        - paragraph [ref=e28]: Operations
        - generic [ref=e29]:
          - button "Orders 12" [ref=e30] [cursor=pointer]:
            - generic [ref=e31]: Orders
            - strong [ref=e32]: "12"
          - button "Inventory 8" [ref=e33] [cursor=pointer]:
            - generic [ref=e34]: Inventory
            - strong [ref=e35]: "8"
          - button "Catalog 48" [ref=e36] [cursor=pointer]:
            - generic [ref=e37]: Catalog
            - strong [ref=e38]: "48"
          - button "Locations 2" [ref=e39] [cursor=pointer]:
            - generic [ref=e40]: Locations
            - strong [ref=e41]: "2"
          - button "Fulfillment 32" [ref=e42] [cursor=pointer]:
            - generic [ref=e43]: Fulfillment
            - strong [ref=e44]: "32"
      - generic [ref=e45]:
        - paragraph [ref=e46]: Control
        - generic [ref=e47]:
          - button "Scenarios 1" [ref=e48] [cursor=pointer]:
            - generic [ref=e49]: Scenarios
            - strong [ref=e50]: "1"
          - button "Scenario History 12" [ref=e51] [cursor=pointer]:
            - generic [ref=e52]: Scenario History
            - strong [ref=e53]: "12"
          - button "Approvals 1" [ref=e54] [cursor=pointer]:
            - generic [ref=e55]: Approvals
            - strong [ref=e56]: "1"
          - button "Escalations 8" [ref=e57] [cursor=pointer]:
            - generic [ref=e58]: Escalations
            - strong [ref=e59]: "8"
      - generic [ref=e60]:
        - paragraph [ref=e61]: Systems
        - generic [ref=e62]:
          - button "Integrations 14" [ref=e63] [cursor=pointer]:
            - generic [ref=e64]: Integrations
            - strong [ref=e65]: "14"
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
          - button "System Config 0" [ref=e93] [cursor=pointer]:
            - generic [ref=e94]: System Config
            - strong [ref=e95]: "0"
          - button "Releases 1" [ref=e96] [cursor=pointer]:
            - generic [ref=e97]: Releases
            - strong [ref=e98]: "1"
    - generic [ref=e99]:
      - generic [ref=e102]: Realtime Reconnecting
      - button "Profile & Session" [ref=e103] [cursor=pointer]
  - generic [ref=e104]:
    - banner [ref=e105]:
      - generic [ref=e106]:
        - paragraph [ref=e107]: CONTROL
        - heading "Scenario history and compare" [level=1] [ref=e108]
        - paragraph [ref=e109]: Track previous scenarios, reload them into the planner, and compare them against the live operating state.
      - generic [ref=e110]:
        - generic [ref=e112]:
          - generic [ref=e113]: Global search
          - textbox "Global search" [ref=e114]:
            - /placeholder: Search pages, orders, or alerts
        - generic "Page focus" [ref=e115]:
          - generic [ref=e116] [cursor=pointer]: Saved plans
          - generic [ref=e117] [cursor=pointer]: Revision flow
          - generic [ref=e118] [cursor=pointer]: Compare history
        - generic [ref=e119]:
          - generic [ref=e120]: 2026/04/29, 23:41:56
          - generic [ref=e121]: Reconnecting
          - button "Notifications 13" [ref=e122] [cursor=pointer]
        - generic [ref=e123]:
          - button "Open scenarios" [ref=e124] [cursor=pointer]
          - button "Open approvals" [ref=e125] [cursor=pointer]
          - button "Refresh" [ref=e126] [cursor=pointer]
          - button "Hosted Verification Tenant Admin" [ref=e127] [cursor=pointer]
          - button "Sign Out" [ref=e128] [cursor=pointer]
    - generic [ref=e129]:
      - main [ref=e130]:
        - generic [ref=e131]:
          - generic [ref=e132]:
            - paragraph [ref=e133]: Current page
            - heading "Scenario History" [level=2] [ref=e134]
            - paragraph [ref=e135]: 12 scenario runs in view
          - generic [ref=e136]:
            - generic [ref=e137]: Saved plans
            - generic [ref=e138]: Revision flow
            - generic [ref=e139]: Compare history
        - article [ref=e141]:
          - generic [ref=e142]:
            - generic [ref=e143]:
              - paragraph [ref=e144]: Control center
              - heading "Planning, approvals, and escalations" [level=2] [ref=e145]
            - generic [ref=e146]: "12"
          - generic [ref=e147]:
            - article [ref=e148]:
              - text: Pending approvals
              - strong [ref=e149]: "1"
            - article [ref=e150]:
              - text: Approved
              - strong [ref=e151]: "5"
            - article [ref=e152]:
              - text: Rejected
              - strong [ref=e153]: "0"
            - article [ref=e154]:
              - text: Overdue
              - strong [ref=e155]: "0"
          - generic [ref=e157]:
            - generic [ref=e158]:
              - strong [ref=e159]: UI Scenario 655D9FE7
              - generic [ref=e161]: Critical
            - paragraph [ref=e162]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority CRITICAL (score 40) with 0 critical exposures and 0 low-stock items.
            - paragraph [ref=e163]: WH-NORTH | Pending Review | Review owner Operations Lead
        - article [ref=e165]:
          - generic [ref=e166]:
            - generic [ref=e167]:
              - paragraph [ref=e168]: Scenario history
              - heading "Saved plans, revisions, and compare posture" [level=2] [ref=e169]
            - generic [ref=e170]: "12"
          - generic [ref=e171]:
            - article [ref=e172]:
              - text: Saved plans
              - strong [ref=e173]: "6"
            - article [ref=e174]:
              - text: Comparisons
              - strong [ref=e175]: "0"
            - article [ref=e176]:
              - text: Revisions
              - strong [ref=e177]: "6"
            - article [ref=e178]:
              - text: Executable
              - strong [ref=e179]: "5"
          - generic [ref=e180]:
            - button "UI Scenario 655D9FE7 Saved Plan Rev 1 Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority CRITICAL (score 40) with 0 critical exposures and 0 low-stock items. Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH | 2026/04/29, 23:41:54" [ref=e181] [cursor=pointer]:
              - generic [ref=e182]:
                - strong [ref=e183]: UI Scenario 655D9FE7
                - generic [ref=e184]:
                  - generic [ref=e185]: Saved Plan
                  - generic [ref=e186]: Rev 1
              - paragraph [ref=e187]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority CRITICAL (score 40) with 0 critical exposures and 0 low-stock items.
              - paragraph [ref=e188]: Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH | 2026/04/29, 23:41:54
            - 'button "Executed scenario: UI Scenario 5921F8DB Execution Created live order ORD-10C2C8D0 for 1 units in WH-NORTH. ORD-10C2C8D0 | 2026/04/29, 23:33:52" [ref=e189] [cursor=pointer]':
              - generic [ref=e190]:
                - strong [ref=e191]: "Executed scenario: UI Scenario 5921F8DB"
                - generic [ref=e193]: Execution
              - paragraph [ref=e194]: Created live order ORD-10C2C8D0 for 1 units in WH-NORTH.
              - paragraph [ref=e195]: ORD-10C2C8D0 | 2026/04/29, 23:33:52
            - button "UI Scenario 5921F8DB Saved Plan Rev 1 Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority HIGH (score 40) with 0 critical exposures and 0 low-stock items. Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH | 2026/04/29, 23:33:42" [ref=e196] [cursor=pointer]:
              - generic [ref=e197]:
                - strong [ref=e198]: UI Scenario 5921F8DB
                - generic [ref=e199]:
                  - generic [ref=e200]: Saved Plan
                  - generic [ref=e201]: Rev 1
              - paragraph [ref=e202]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority HIGH (score 40) with 0 critical exposures and 0 low-stock items.
              - paragraph [ref=e203]: Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH | 2026/04/29, 23:33:42
            - 'button "Executed scenario: UI Scenario 77AE52FB Execution Created live order ORD-4A95D443 for 1 units in WH-NORTH. ORD-4A95D443 | 2026/04/29, 23:15:10" [ref=e204] [cursor=pointer]':
              - generic [ref=e205]:
                - strong [ref=e206]: "Executed scenario: UI Scenario 77AE52FB"
                - generic [ref=e208]: Execution
              - paragraph [ref=e209]: Created live order ORD-4A95D443 for 1 units in WH-NORTH.
              - paragraph [ref=e210]: ORD-4A95D443 | 2026/04/29, 23:15:10
            - button "UI Scenario 77AE52FB Saved Plan Rev 1 Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority HIGH (score 40) with 0 critical exposures and 0 low-stock items. Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH | 2026/04/29, 23:14:57" [ref=e211] [cursor=pointer]:
              - generic [ref=e212]:
                - strong [ref=e213]: UI Scenario 77AE52FB
                - generic [ref=e214]:
                  - generic [ref=e215]: Saved Plan
                  - generic [ref=e216]: Rev 1
              - paragraph [ref=e217]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority HIGH (score 40) with 0 critical exposures and 0 low-stock items.
              - paragraph [ref=e218]: Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH | 2026/04/29, 23:14:57
            - 'button "Executed scenario: UI Scenario 35DE5EA2 Execution Created live order ORD-AB08F750 for 1 units in WH-NORTH. ORD-AB08F750 | 2026/04/29, 21:26:49" [ref=e219] [cursor=pointer]':
              - generic [ref=e220]:
                - strong [ref=e221]: "Executed scenario: UI Scenario 35DE5EA2"
                - generic [ref=e223]: Execution
              - paragraph [ref=e224]: Created live order ORD-AB08F750 for 1 units in WH-NORTH.
              - paragraph [ref=e225]: ORD-AB08F750 | 2026/04/29, 21:26:49
          - generic [ref=e226]:
            - article [ref=e227]:
              - generic [ref=e228]:
                - strong [ref=e229]: Selected scenario memory
                - generic [ref=e230]: Saved Plan
              - generic [ref=e232]:
                - strong [ref=e233]: UI Scenario 655D9FE7
                - paragraph [ref=e234]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority CRITICAL (score 40) with 0 critical exposures and 0 low-stock items.
                - paragraph [ref=e235]: WH-NORTH | Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                - paragraph [ref=e236]: Approval Pending Approval | Policy Escalated | Stage Pending Review
                - paragraph [ref=e237]: Requested by Operations Lead | Review owner Operations Lead | Final approver Executive Operations Director
                - paragraph [ref=e238]: Due 2026/04/30, 01:41:54
                - paragraph [ref=e239]: Revision 1
            - article [ref=e240]:
              - generic [ref=e241]:
                - strong [ref=e242]: Scenario action console
                - generic [ref=e243]: Pending Approval
              - generic [ref=e245]:
                - strong [ref=e246]: UI Scenario 655D9FE7
                - paragraph [ref=e247]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority CRITICAL (score 40) with 0 critical exposures and 0 low-stock items.
                - paragraph [ref=e248]: WH-NORTH | Saved Plan | Critical priority
                - paragraph [ref=e249]: Requested by Operations Lead | Review owner Operations Lead | Final approver Executive Operations Director
                - paragraph [ref=e250]: Approval needs Review Owner | Rejection needs Review Owner | Due 2026/04/30, 01:41:54
              - generic [ref=e251]:
                - generic [ref=e252]:
                  - generic [ref=e253]: Acting As
                  - combobox "Acting As" [ref=e254]:
                    - option "Review Owner" [selected]
                    - option "Final Approver"
                    - option "Escalation Owner"
                - generic [ref=e255]:
                  - generic [ref=e256]: Decision Note
                  - textbox "Decision Note" [ref=e257]:
                    - /placeholder: Recommended for final approval; required for rejection
              - generic [ref=e258]:
                - button "Load Into Planner" [ref=e259] [cursor=pointer]
                - button "Owner Review" [disabled] [ref=e260]
                - button "Reject Plan" [disabled] [ref=e261]
          - generic [ref=e262]:
            - article [ref=e263]:
              - generic [ref=e264]:
                - strong [ref=e265]: Execution-ready plans
                - generic [ref=e266]: "4"
              - generic [ref=e267]:
                - generic [ref=e268]:
                  - strong [ref=e269]: UI Scenario 5921F8DB
                  - paragraph [ref=e270]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority HIGH (score 40) with 0 critical exposures and 0 low-stock items.
                  - paragraph [ref=e271]: Operations Lead | Operations Lead
                - generic [ref=e272]:
                  - strong [ref=e273]: UI Scenario 77AE52FB
                  - paragraph [ref=e274]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority HIGH (score 40) with 0 critical exposures and 0 low-stock items.
                  - paragraph [ref=e275]: Operations Lead | Operations Lead
                - generic [ref=e276]:
                  - strong [ref=e277]: UI Scenario 35DE5EA2
                  - paragraph [ref=e278]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority MEDIUM (score 0) with 0 critical exposures and 0 low-stock items.
                  - paragraph [ref=e279]: Operations Lead | Operations Lead
                - generic [ref=e280]:
                  - strong [ref=e281]: UI Scenario 9E54E10A
                  - paragraph [ref=e282]: Saved plan for WH-NORTH with 1 line items and 1 projected units. Review priority MEDIUM (score 0) with 0 critical exposures and 0 low-stock items.
                  - paragraph [ref=e283]: Operations Lead | Operations Lead
            - article [ref=e284]:
              - generic [ref=e285]:
                - strong [ref=e286]: Revision memory
                - generic [ref=e287]: "4"
              - generic [ref=e288]:
                - generic [ref=e289]:
                  - strong [ref=e290]: UI Scenario 655D9FE7
                  - paragraph [ref=e291]: Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - paragraph [ref=e292]: Rev 1 | 2026/04/29, 23:41:54
                - generic [ref=e293]:
                  - strong [ref=e294]: UI Scenario 5921F8DB
                  - paragraph [ref=e295]: Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - paragraph [ref=e296]: Rev 1 | 2026/04/29, 23:33:42
                - generic [ref=e297]:
                  - strong [ref=e298]: UI Scenario 77AE52FB
                  - paragraph [ref=e299]: Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
                  - paragraph [ref=e300]: Rev 1 | 2026/04/29, 23:14:57
                - generic [ref=e301]:
                  - strong [ref=e302]: UI Scenario 35DE5EA2
                  - paragraph [ref=e303]: Review and execute when ready
                  - paragraph [ref=e304]: Rev 1 | 2026/04/29, 21:26:34
      - complementary [ref=e305]:
        - article [ref=e306]:
          - generic [ref=e307]:
            - paragraph [ref=e308]: Realtime state
            - generic [ref=e309]: Reconnecting
          - strong [ref=e310]: Monitoring live operating state
          - paragraph [ref=e311]: Snapshot 2026/04/29, 23:42:15
          - generic [ref=e312]:
            - generic [ref=e313]:
              - generic [ref=e314]: Alerts
              - strong [ref=e315]: "4"
            - generic [ref=e316]:
              - generic [ref=e317]: Actions
              - strong [ref=e318]: "12"
            - generic [ref=e319]:
              - generic [ref=e320]: Replay
              - strong [ref=e321]: "0"
            - generic [ref=e322]:
              - generic [ref=e323]: Incidents
              - strong [ref=e324]: "8"
        - article [ref=e325]:
          - generic [ref=e326]:
            - paragraph [ref=e327]: Scenario focus
            - generic [ref=e328]: Saved Plan
          - strong [ref=e329]: UI Scenario 655D9FE7
          - paragraph [ref=e330]: WH-NORTH | Pending Approval | Prepare replenishment for SKU SKU-PILOT-TENANT-PROOF at WH-NORTH
          - generic [ref=e331]:
            - generic [ref=e332]:
              - generic [ref=e333]: History
              - strong [ref=e334]: "12"
            - generic [ref=e335]:
              - generic [ref=e336]: Revisions
              - strong [ref=e337]: "6"
            - generic [ref=e338]:
              - generic [ref=e339]: Executable
              - strong [ref=e340]: "5"
            - generic [ref=e341]:
              - generic [ref=e342]: Escalated
              - strong [ref=e343]: "0"
          - button "Quick route Open planner Shift from saved memory back into the decision lab." [ref=e345] [cursor=pointer]:
            - generic [ref=e346]: Quick route
            - strong [ref=e347]: Open planner
            - paragraph [ref=e348]: Shift from saved memory back into the decision lab.
        - article [ref=e349]:
          - generic [ref=e350]:
            - paragraph [ref=e351]: Act now
            - generic [ref=e352]: 5 items
          - generic [ref=e353]:
            - button "Critical Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH Demand is accelerating for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.4 hours at the current demand rate. Review threshold settings and stage replenishment now." [ref=e354] [cursor=pointer]:
              - generic [ref=e355]: Critical
              - strong [ref=e356]: Depletion risk rising for SKU SKU-PILOT-TENANT-PROOF in WH-NORTH
              - paragraph [ref=e357]: Demand is accelerating for PILOT-TENANT Hosted Verification North Hub. Estimated stockout window is 3.4 hours at the current demand rate. Review threshold settings and stage replenishment now.
            - button "Critical Delivery delay risk rising in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 22, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e358] [cursor=pointer]:
              - generic [ref=e359]: Critical
              - strong [ref=e360]: Delivery delay risk rising in WH-NORTH
              - paragraph [ref=e361]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 22, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Fulfillment backlog building in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 22, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e362] [cursor=pointer]:
              - generic [ref=e363]: Critical
              - strong [ref=e364]: Fulfillment backlog building in WH-NORTH
              - paragraph [ref=e365]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 22, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
            - button "Critical Urgent reorder for SKU SKU-RT-B0040E18 at WH-NORTH Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10." [ref=e366] [cursor=pointer]:
              - generic [ref=e367]: Critical
              - strong [ref=e368]: Urgent reorder for SKU SKU-RT-B0040E18 at WH-NORTH
              - paragraph [ref=e369]: Reorder immediately for PILOT-TENANT Hosted Verification North Hub. Current quantity is 5 units versus a threshold of 10.
            - button "Critical Investigate logistics anomaly in WH-NORTH Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 22, delayed shipments are 10, and warehouse operations should investigate the blocked lane now." [ref=e370] [cursor=pointer]:
              - generic [ref=e371]: Critical
              - strong [ref=e372]: Investigate logistics anomaly in WH-NORTH
              - paragraph [ref=e373]: Exceptions or repeated delivery slowdowns are stacking in PILOT-TENANT Hosted Verification North Hub. Backlog is 22, delayed shipments are 10, and warehouse operations should investigate the blocked lane now.
        - article [ref=e374]:
          - generic [ref=e375]:
            - paragraph [ref=e376]: Activity stream
            - generic [ref=e377]: "6"
          - generic [ref=e378]:
            - generic [ref=e379]:
              - strong [ref=e380]: UI Replay 322EDAF1 degraded
              - paragraph [ref=e381]: Medium incident
              - generic [ref=e382]: 2026/04/29, 23:41:37
            - generic [ref=e383]:
              - strong [ref=e384]: Csv Order Import
              - paragraph [ref=e385]: High incident
              - generic [ref=e386]: 2026/04/29, 23:41:37
            - generic [ref=e387]:
              - strong [ref=e388]: Scenario Saved
              - paragraph [ref=e389]: Saved plan UI Scenario 655D9FE7 for warehouse WH-NORTH with CRITICAL review priority (score 40).
              - generic [ref=e390]: 2026/04/29, 23:41:54
            - generic [ref=e391]:
              - strong [ref=e392]: Integration Replay Completed
              - paragraph [ref=e393]: Replayed failed CSV_ORDER_IMPORT order UI-RPL-322EDAF1 from ui_replay_322edaf1 by Integration Lead.
              - generic [ref=e394]: 2026/04/29, 23:41:49
            - generic [ref=e395]:
              - strong [ref=e396]: Integration Replay Completed
              - paragraph [ref=e397]: IntegrationReplayRecord | 14
              - generic [ref=e398]: 2026/04/29, 23:41:49
            - generic [ref=e399]:
              - strong [ref=e400]: Order Processed
              - paragraph [ref=e401]: CustomerOrder | UI-RPL-322EDAF1
              - generic [ref=e402]: 2026/04/29, 23:41:49
        - article [ref=e403]:
          - generic [ref=e404]:
            - paragraph [ref=e405]: Operator
            - generic [ref=e406]: Healthy
          - strong [ref=e407]: Hosted Verification Tenant Admin
          - paragraph [ref=e408]: PILOT-TENANT Hosted Verification | Operations Lead
          - paragraph [ref=e409]: Roles Escalation Owner, Integration Admin, Integration Operator, Review Owner, Tenant Admin
          - paragraph [ref=e410]: Warehouse scope Tenant-wide
```

# Test source

```ts
  677 |           return 'replayed'
  678 |         }
  679 |         await refreshWorkspace(page)
  680 |         return await replayQueueRecord.isVisible().catch(() => false) ? 'visible' : 'waiting'
  681 |       }, {
  682 |         timeout: 30_000,
  683 |         message: `Expected replay queue ${replayFixture.externalOrderId} to appear in the UI or auto-recover before manual replay.`,
  684 |       }).not.toBe('waiting')
  685 | 
  686 |       if (currentReplayOutcome.state === 'replayed') {
  687 |         await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  688 |       } else {
  689 |         await expect(replayQueueRecord).toBeVisible()
  690 |         await replayQueueRecord.click()
  691 | 
  692 |         const replayDetail = page.locator('.section-card').filter({ hasText: 'Recovery detail' }).first()
  693 |         await expect(replayDetail.getByText(replayFixture.externalOrderId).first()).toBeVisible()
  694 | 
  695 |         const replayButton = replayDetail.getByRole('button', { name: 'Replay Into Live Flow' })
  696 |         await expect(replayButton).toBeVisible()
  697 |         await expect(replayButton).toBeEnabled()
  698 | 
  699 |         const replayResponsePromise = page.waitForResponse((response) => (
  700 |           response.request().method() === 'POST'
  701 |             && /\/api\/integrations\/orders\/replay\/\d+$/i.test(response.url())
  702 |         ), { timeout: 20_000 })
  703 | 
  704 |         let replayResponse = null
  705 |         try {
  706 |           await replayButton.scrollIntoViewIfNeeded()
  707 |           ;[replayResponse] = await Promise.all([
  708 |             replayResponsePromise,
  709 |             replayButton.click(),
  710 |           ])
  711 |         } catch (error) {
  712 |           currentReplayOutcome = await readReplayOutcome(replayFixture.api, replayFixture.externalOrderId)
  713 |           if (currentReplayOutcome.state !== 'replayed') {
  714 |             throw error
  715 |           }
  716 |         }
  717 | 
  718 |         if (replayResponse) {
  719 |           const replayPayload = await replayResponse.json().catch(() => null)
  720 |           if (!replayResponse.ok()) {
  721 |             const replayFailureMessage = replayPayload?.message
  722 |               || `Replay request failed with status ${replayResponse.status()} for ${replayFixture.externalOrderId}.`
  723 | 
  724 |             let replayResolvedAfterConflict = false
  725 |             try {
  726 |               await waitForReplayResolution(
  727 |                 replayFixture.api,
  728 |                 replayFixture.externalOrderId,
  729 |                 20_000,
  730 |                 `Expected ${replayFixture.externalOrderId} to settle into a replayed state after replay response ${replayResponse.status()}.`,
  731 |               )
  732 |               replayResolvedAfterConflict = true
  733 |             } catch {
  734 |               replayResolvedAfterConflict = false
  735 |             }
  736 | 
  737 |             if (!replayResolvedAfterConflict) {
  738 |               throw new Error(replayFailureMessage)
  739 |             }
  740 | 
  741 |             await refreshWorkspace(page)
  742 |             await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  743 |           } else {
  744 |             await expect(page.locator('.success-text').filter({
  745 |               hasText: `Replayed ${replayFixture.externalOrderId} into the live order flow.`,
  746 |             }).first()).toBeVisible()
  747 |           }
  748 |         }
  749 |       }
  750 |     }
  751 | 
  752 |     await waitForReplayResolution(
  753 |       replayFixture.api,
  754 |       replayFixture.externalOrderId,
  755 |       60_000,
  756 |       `Expected ${replayFixture.externalOrderId} to reach a replayed state through manual or automated recovery.`,
  757 |     )
  758 | 
  759 |   await expect(page.getByText(/Replay queue is clear|Replayed .* into the live order flow\./).first()).toBeVisible()
  760 |   } finally {
  761 |     await replayFixture.api.dispose()
  762 |   }
  763 | 
  764 |   await signOutViaUi(page)
  765 | 
  766 |   const scenarioFixture = await createScenarioFixture()
  767 | 
  768 |   try {
  769 |     await loginViaUi(page, users.operationsLead)
  770 |     await navigateWithinApp(page, '/scenario-history')
  771 |     await expect(page.getByRole('heading', { level: 1, name: 'Scenario history and compare' })).toBeVisible()
  772 | 
  773 |     const scenarioApprovalConsole = page.locator('.stack-card').filter({
  774 |       hasText: scenarioFixture.title,
  775 |       has: page.getByRole('button', { name: 'Approve Plan' }),
  776 |     }).first()
> 777 |     await expect(scenarioApprovalConsole).toBeVisible()
      |                                           ^ Error: expect(locator).toBeVisible() failed
  778 |     await scenarioApprovalConsole.getByRole('button', { name: 'Approve Plan' }).click()
  779 |     await expect(page.locator('.success-text').filter({ hasText: `Approved ${scenarioFixture.title} for execution under Standard approval.` }).first()).toBeVisible()
  780 | 
  781 |     const scenarioExecutionConsole = page.locator('.stack-card').filter({
  782 |       hasText: scenarioFixture.title,
  783 |       has: page.getByRole('button', { name: 'Execute Scenario' }),
  784 |     }).first()
  785 |     await expect(scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' })).toBeVisible()
  786 |     await scenarioExecutionConsole.getByRole('button', { name: 'Execute Scenario' }).click()
  787 |     await expect(page.locator('.success-text').filter({ hasText: new RegExp(`^Executed ${scenarioFixture.title} as live order `, 'i') }).first()).toBeVisible()
  788 |   } finally {
  789 |     await scenarioFixture.api.dispose()
  790 |   }
  791 | 
  792 |   await signOutViaUi(page)
  793 | 
  794 |   await loginViaUi(page, users.operationsPlanner)
  795 |   await navigateWithinApp(page, '/users')
  796 |   await expect(page.getByRole('heading', { level: 1, name: 'Users and access control' })).toBeVisible()
  797 |   await expect(page.getByText('Tenant admin access required')).toBeVisible()
  798 |   await expect(page.getByText('Operators', { exact: true }).first()).toBeVisible()
  799 | })
  800 | 
  801 | test('alerts, recommendations, orders, inventory, integrations, users, profile, and settings surfaces stay connected to the live backend', async ({ page }) => {
  802 |   const api = await createApiContext(users.operationsLead)
  803 |   let restoreAlertCoverage = async () => {}
  804 | 
  805 |   try {
  806 |     const alertCoverage = await ensureAlertAndRecommendationCoverage(api)
  807 |     restoreAlertCoverage = alertCoverage.restore
  808 |     const recentOrders = await ensureRecentOrder(api)
  809 |     const workspace = await readJson(await api.get('/api/access/admin/workspace'))
  810 |     const operators = await readJson(await api.get('/api/access/admin/operators'))
  811 |     const accessUsers = await readJson(await api.get('/api/access/admin/users'))
  812 |     const alertRecord = alertCoverage.alertRecord
  813 |     const recommendationRecord = alertCoverage.recommendationRecord
  814 |     const orderRecord = recentOrders[0]
  815 |     const inventoryRecord = alertCoverage.snapshot.inventory.find((item) => item.lowStock) || alertCoverage.snapshot.inventory[0]
  816 |     const connectorCandidates = [
  817 |       ...alertCoverage.snapshot.integrationConnectors,
  818 |       ...(workspace.connectors || []),
  819 |     ]
  820 | 
  821 |     expect(alertRecord).toBeTruthy()
  822 |     expect(recommendationRecord).toBeTruthy()
  823 |     expect(orderRecord).toBeTruthy()
  824 |     expect(inventoryRecord).toBeTruthy()
  825 |     expect(workspace).toBeTruthy()
  826 |     expect(operators.length).toBeGreaterThan(0)
  827 |     expect(accessUsers.length).toBeGreaterThan(0)
  828 |     expect(connectorCandidates.length).toBeGreaterThan(0)
  829 | 
  830 |     await loginViaUi(page, users.operationsLead)
  831 | 
  832 |     await navigateWithinApp(page, '/alerts')
  833 |     await expect(page.getByRole('heading', { level: 1, name: 'Operational warning center' })).toBeVisible()
  834 |     await expect(page.getByText(alertRecord.title).first()).toBeVisible()
  835 |     await activateSelectableButton(
  836 |       page.getByRole('button', { name: new RegExp(escapeRegExp(alertRecord.title), 'i') }).first(),
  837 |     )
  838 |     await expect(page.getByText(`Action: ${alertRecord.recommendedAction}`).first()).toBeVisible()
  839 | 
  840 |     await navigateWithinApp(page, '/recommendations')
  841 |     await expect(page.getByRole('heading', { level: 1, name: 'Action queue for the operating team' })).toBeVisible()
  842 |     await expect(page.getByText(recommendationRecord.title).first()).toBeVisible()
  843 |     await activateSelectableButton(
  844 |       page.locator('.recommendation-board').getByRole('button', { name: new RegExp(escapeRegExp(recommendationRecord.title), 'i') }).first(),
  845 |     )
  846 |     await expect(page.getByText(recommendationRecord.description).first()).toBeVisible()
  847 | 
  848 |     await navigateWithinApp(page, '/orders')
  849 |     await expect(page.getByRole('heading', { level: 1, name: 'Live order operations' })).toBeVisible()
  850 |     await expect(page.getByText(orderRecord.externalOrderId).first()).toBeVisible()
  851 |     await activateSelectableButton(
  852 |       page.getByRole('button', { name: new RegExp(escapeRegExp(orderRecord.externalOrderId), 'i') }).first(),
  853 |     )
  854 |     await expect(page.getByText(orderRecord.warehouseCode).first()).toBeVisible()
  855 | 
  856 |     await navigateWithinApp(page, '/inventory')
  857 |     await expect(page.getByRole('heading', { level: 1, name: 'Inventory intelligence' })).toBeVisible()
  858 |     await expect(page.getByText(inventoryRecord.productName).first()).toBeVisible()
  859 |     await activateSelectableButton(
  860 |       page.getByRole('button', { name: new RegExp(escapeRegExp(inventoryRecord.productName), 'i') }).first(),
  861 |     )
  862 |     await expect(page.getByText(inventoryRecord.productSku).first()).toBeVisible()
  863 | 
  864 |     await navigateWithinApp(page, '/integrations')
  865 |     await expect(page.getByRole('heading', { level: 1, name: 'Connector management and telemetry' })).toBeVisible()
  866 |     await expect(page.locator('button.system-select-card').first()).toBeVisible()
  867 |     const visibleConnectorMatch = await findVisibleIntegrationConnector(page, connectorCandidates)
  868 |     if (!visibleConnectorMatch) {
  869 |       throw new Error('Expected at least one integration connector rendered in the UI to match backend connector data.')
  870 |     }
  871 |     await activateSelectableButton(
  872 |       visibleConnectorMatch.button,
  873 |     )
  874 |     await expect(page.getByText(visibleConnectorMatch.connector.sourceSystem).first()).toBeVisible()
  875 |     await page.getByRole('button', { name: 'Manage Policies' }).click()
  876 | 
  877 |     await expect(page.getByRole('heading', { level: 1, name: 'Tenant and workspace settings' })).toBeVisible()
```