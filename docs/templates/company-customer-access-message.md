# Company Customer Access Message Template

Use this short message only after the actual company Phase 8 decision authorizes handover and the internal pre-send checklist passes.

Do not add a password, token, session value, backend URL, internal API, or platform information. The initial secret is delivered separately through the approved secure channel.

## Subject

```text
SynapseCore pilot access for [COMPANY]
```

## Message

```text
Hello [RECIPIENT NAME],

Your approved SynapseCore pilot access for [COMPANY] is ready.

Open SynapseCore:
[APPROVED FRONTEND URL]

Company workspace code:
[WORKSPACE CODE]

Assigned username:
[USERNAME]

Role summary:
[CUSTOMER-READABLE ROLE SUMMARY]

Your initial secret will be delivered separately through the approved secure
channel. Do not share it or place it in email, chat, screenshots, or support
tickets.

At first login, confirm the company workspace and your identity. If Profile shows
Password change required, change the password before operational use.

Pilot start reference:
[PILOT START DATE OR REFERENCE]

Support:
[APPROVED SUPPORT CONTACT OR CHANNEL]

If the tenant, role, data, or access looks wrong, stop and contact support before
performing operational actions.

Regards,
[SYNAPSCORE HANDOVER OWNER]
```

## Internal Send Record

Do not include this section in the customer message.

| Field | Value |
| --- | --- |
| Phase 8 authorization reference |  |
| Recipient identity verified | YES / NO |
| Identity message delivered | YES / NO |
| Secret delivered separately | PENDING / DELIVERED / CONFIRMED / RESET REQUIRED / REVOKED |
| Delivery date/time |  |
| Handover record reference |  |
