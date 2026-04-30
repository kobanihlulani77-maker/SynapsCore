# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: prod-proof.spec.mjs >> frontend surfaces backend auth rate limiting without getting stuck in a loading state
- Location: tests\prod-proof.spec.mjs:945:1

# Error details

```
Error: page.evaluate: TypeError: Failed to fetch
    at eval (eval at evaluate (:302:30), <anonymous>:12:30)
    at async <anonymous>:328:30
```

# Page snapshot

```yaml
- main [ref=e3]:
  - generic [ref=e4]:
    - button "S SynapseCore Operational intelligence operating system" [ref=e5] [cursor=pointer]:
      - generic [ref=e6]: S
      - generic [ref=e7]:
        - strong [ref=e8]: SynapseCore
        - generic [ref=e9]: Operational intelligence operating system
    - navigation [ref=e10]:
      - button "Home" [ref=e11] [cursor=pointer]
      - button "Product" [ref=e12] [cursor=pointer]
      - button "Contact" [ref=e13] [cursor=pointer]
  - generic [ref=e14]:
    - article [ref=e15]:
      - paragraph [ref=e16]: Secure company entry
      - heading "Access your operational workspace." [level=1] [ref=e17]
      - paragraph [ref=e18]: Sign in to the right company workspace and move from visibility to action without leaving the control center.
      - generic [ref=e19]:
        - generic [ref=e20]: 3 workspaces visible
        - generic [ref=e21]: Realtime path ready
      - generic [ref=e22]:
        - article [ref=e23]:
          - strong [ref=e24]: Live visibility
          - paragraph [ref=e25]: Orders, inventory, locations, fulfillment, incidents, and connectors pulled into one operational picture.
        - article [ref=e26]:
          - strong [ref=e27]: Prediction and guidance
          - paragraph [ref=e28]: Detect risk early, estimate near-term impact, and surface what the team should do next.
        - article [ref=e29]:
          - strong [ref=e30]: Control and trust
          - paragraph [ref=e31]: Run scenarios, route approvals, recover failed inbound work, and keep runtime confidence visible.
    - article [ref=e32]:
      - paragraph [ref=e33]: Company sign in
      - heading "Enter the operational platform" [level=2] [ref=e34]
      - generic [ref=e35]:
        - generic [ref=e36]:
          - generic [ref=e37]:
            - generic [ref=e38]: Tenant workspace
            - combobox "Tenant workspace" [ref=e39]: PILOT-CANARY-423
          - generic [ref=e40]:
            - generic [ref=e41]: Username
            - textbox "Username" [ref=e42]:
              - /placeholder: workspace.admin
          - generic [ref=e43]:
            - generic [ref=e44]: Password
            - textbox "Password" [ref=e45]:
              - /placeholder: Enter workspace password
        - generic [ref=e46]:
          - generic [ref=e47]:
            - checkbox "Remember tenant code and username on this device" [checked] [ref=e48]
            - generic [ref=e49]: Remember tenant code and username on this device
          - generic [ref=e50]: Password recovery is managed by your tenant admin.
        - generic [ref=e51]:
          - button "Enter Platform" [disabled] [ref=e52]
          - button "Product Overview" [ref=e53] [cursor=pointer]
      - generic [ref=e54]:
        - article [ref=e55]:
          - generic [ref=e56]: Tenant scope
          - strong [ref=e57]: Pilot Canary 423
          - paragraph [ref=e58]: Operators enter a company workspace, not a generic app account.
        - article [ref=e59]:
          - generic [ref=e60]: Session model
          - strong [ref=e61]: Secure browser session
          - paragraph [ref=e62]: Protected actions, approvals, replay, and realtime access all follow the signed-in operator identity.
        - article [ref=e63]:
          - generic [ref=e64]: Realtime posture
          - strong [ref=e65]: Live transport configured
          - paragraph [ref=e66]: SynapseCore opens the command workspace with live operational updates when the session is valid.
      - paragraph [ref=e67]: Signing into Pilot Canary 423.
      - paragraph [ref=e68]: API https://synapscore-3.onrender.com | Realtime wss://synapscore-3.onrender.com/ws | Transport undefined
```