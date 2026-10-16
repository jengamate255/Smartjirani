# SmartJirani Product Requirements Document (PRD)

---

## 1. Executive Summary & Vision

**Product Name:** SmartJirani  
**Tagline:** *"Mtu Ni Watu"* — Street-based community trust, mutual aid & hyper-local governance.  
**Platform Target:** Mobile (iOS / Android / PWA)  
**Core Cultural Concept:** Rooted in the African philosophy of **Ujamaa** (familyhood/togetherness) and **Harambee** (pulling together). SmartJirani bridges traditional hyper-local Kenyan/East African neighborhood structures (such as *Nyumba Kumi* - 10-house clusters, street barazas, and estate associations) with modern mobile technology.

### Problem Statement
Modern urban and suburban residential communities (e.g., Nairobi estates like Kileleshwa, Kilimani, Buruburu, and gated courts across East Africa) suffer from:
1. **Fragmented Communication:** Chaotic, unmoderated WhatsApp and Telegram groups plagued by spam, misinformation, and lack of accountability.
2. **Underutilized Mutual Aid:** Neighbors own expensive tools (drills, ladders, lawnmowers) that sit idle, while nearby residents must buy or rent them at high cost.
3. **Low Civic Participation:** Poor quorum and coordination for estate projects (security gates, solar street lighting, pothole repairs, tree planting).
4. **Security & Emergency Latency:** Slow, disconnected incident reporting during water cutoffs, blackout crises, or medical/safety emergencies.

### Value Proposition
SmartJirani digitizes the physical street committee into a verified, high-trust digital ecosystem that facilitates **hyper-local awareness (Mtaa Feed)**, **resource sharing without monetary exploitation (Ujamaa Hub)**, **collective civic development (Baraza & Projects)**, and **reputation-backed neighbor identity (Resident Profile & Karma Pass)**.

---

## 2. Target Audience & Personas

| Persona | Role | Primary Goals & Motivations |
| :--- | :--- | :--- |
| **Amina (Active Resident & Lender)** | Long-time tenant / homeowner, young professional | Borrow/lend household tools, join weekend cleanups, share surplus kitchen garden herbs, earn trust karma. |
| **Baba Brian (Nyumba Kumi Street Elder)** | Cluster representative, estate committee member | Broadcast verified utility notices, maintain quorum for monthly assemblies, review infrastructure tenders. |
| **Dr. Grace (Emergency / Specialist Neighbor)** | Medical doctor / first responder resident | Answer urgent medical/incident pings within a 200m radius, coordinate pediatric emergencies. |
| **Kevin (Daily Commuter / Working Parent)** | Resident with daily office commute | Offer empty carpool seats along Ring Road / Upper Hill to reduce estate traffic and build friendships. |

---

## 3. Core Product Pillars & Architecture

```
               ┌────────────────────────────────────────────────────────┐
               │                  SmartJirani Platform                  │
               └────────────────────────────────────────────────────────┘
                                           │
         ┌───────────────────┬─────────────┴───────┬────────────────────┐
         │                   │                     │                    │
┌─────────────────┐ ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐
│ 1. Mtaa Feed    │ │ 2. Ujamaa Hub   │  │ 3. Baraza Forum │  │ 4. Resident Trust│
│ - Street Status │ │ - Mutual Aid    │  │ - Townhalls     │  │ - Karma Rating   │
│ - Verified Alert│ │ - Tool Library  │  │ - Harambee Crowd│  │ - Locker/Skills  │
│ - Citizen Polls │ │ - Carpool Barter│  │ - Civic Petition│  │ - SOS Security   │
│ - Produce Barter│ │ - M-Pesa Pools  │  │ - Contractor Bid│  │ - Nyumba Verified│
└─────────────────┘ └─────────────────┘  └─────────────────┘  └──────────────────┘
```

---

## 4. Detailed Feature Specifications

### 4.1 Screen 1: Mtaa Feed (Hyper-Local Street Pulse)
*Primary Goal: Ambient street situational awareness and immediate peer-to-peer sharing.*

1. **Ambient Street Health Bar:**
   - Real-time weather (`24°C Ole Dume Rd`).
   - Grid status tracker (`KPLC: Normal / Outage Reported`).
   - Gate status indicator (`Gate 3 Open / Controlled Access`).
2. **Nyumba Kumi Quick Action Drawer:**
   - One-tap triggers: `Announce`, `Alert Nyumba Kumi`, `Utility Update`.
   - Elder-verified emergency banners (e.g., water tanker maintenance, scheduled fumigation).
3. **Mtaa Collective Voice (Street Polling):**
   - Micro-democracy polling cards with verified resident voter count and closing countdown timers.
   - Example: *"Proposed Saturday clean-up of the riverside path — which time works best for you?"*
4. **Neighbor Surplus & Kitchen Garden Exchange:**
   - Photo-supported zero-cost sharing cards (e.g., fresh organic spinach/mint harvest).
   - "Request Portion" button with quantity countdown reserve.
5. **Estate Event Banners:**
   - Annual General Baraza date, Zoom/in-person hybrid venue, and "Add to Calendar" synchronization.

---

### 4.2 Screen 2: Ujamaa Hub (Mutual Aid, Tool Sharing & Barter)
*Primary Goal: Democratize access to communal goods, urgent help, and crowd-funded estate resilience.*

1. **Harambee Spirit Impact Counter:**
   - Metrics celebrating non-monetary generosity: `38 Neighbors Helped this month`, `0 KSh charged (Pure Ujamaa)`.
2. **Urgent Neighborhood Need (Beacon Alert):**
   - High-priority medical or home emergency callout (e.g., *"Pediatric nebulizer needed for 2 hours for baby neighbor"*).
   - Distance indicator (`80m away`), requester verification, direct call and "I Can Help With This" response button.
3. **Communal Equipment Locker (Borrow & Share):**
   - Categories: Tools & Gear, Ride Shares, Household, Gardening.
   - Item cards featuring owner location, tenure, loan history, availability status, and booking CTA (e.g., Bosch Power Drill, 3.5m Aluminium Step Ladder).
4. **Lane Carpool Matcher:**
   - Scheduled shared morning commutes (e.g., Upper Hill via Ring Road) showing departure time, open seat count, and "Join Carpool" fuel-barter pact.
5. **Emergency & Guard Fund (M-Pesa Micro-Harambee):**
   - Transparent pool progress bar (e.g., `KES 42,500 / 50,000 raised for Solar gate lighting`).
   - One-tap direct STK push integration: `Contribute with M-Pesa`, accompanied by verified audit transparency sheet.

---

### 4.3 Screen 3: Baraza & Events (Townhall, Civic Voice & Projects)
*Primary Goal: Transparent community governance, participatory budgeting, and estate project management.*

1. **Mtaa Council Overview:**
   - Zone definition (`Zone 4B`), registered households count (`142 Reg.`), and constitutional quorum tracker (`84% Safe`).
2. **Next Townhall Assembly (Hybrid Baraza):**
   - Upcoming date/time badge, live stream status indicator, RSVP counter (`68 neighbors attending`).
   - Queue question submission (`12 questions queued`), structured agenda review (CCTV backhaul, waste collection vendor renewal).
3. **Harambee Projects Ledger:**
   - Active infrastructure milestones (e.g., *Solar Streetlight Installation - Phase 2*).
   - Real-time funding progress (`KES 240,000 / 320,000 raised`), volunteer roster slot counter, and `Join Bee` action.
4. **Street Baraza Debate Forum:**
   - Tagged petition cards (`Petitions`, `Utility`, `Security`).
   - Direct county council advocacy threads (e.g., speed bumps near kindergarten gate).

---

### 4.4 Screen 4: Resident Profile & Jirani Trust Karma
*Primary Goal: Verifiable, non-invasive digital neighborhood identity and safety guardrails.*

1. **Verification Credential:**
   - Headshot, verified resident since year, street address badge (*"Street Verified: Nyumba Kumi Approved"*).
   - Neighborhood bio and status.
2. **Ujamaa Trust Karma Score:**
   - Dynamic trust score (`4.9 / 5.0 ★`, `98% Trust Rating`), computed from 44 confirmed neighborhood actions.
   - Score breakdown: Favors Done, Shared Items, Baraza Attendance.
3. **Shared Locker & Skills Portfolio:**
   - Tabbed view: `Items I Share`, `Skills I Offer`, `Reviews`.
   - Inventory item cards with lending counts and toggle states (`Ready` vs `Borrowed`).
4. **Street Security & Emergency Circle:**
   - Designated security unit pairing (`Ole Dume Cluster 3`).
   - Instant 1-tap SOS beacon connected directly to Cluster Elders and Gate Security.
5. **Granular Privacy & Household Controls:**
   - Radial visibility settings (`Within 500m only`).
   - Masked house numbers visible exclusively to verified neighbors.
   - Registered household member management.
6. **Exportable Jirani Karma Pass:**
   - Digital credential verifying good neighbor standing for estate leasing, credit unions (Chamas), and local committees.

---

## 5. Non-Functional Requirements & Security

1. **Zero-Trust Verification Protocol:**
   - Address validation requires approval from an active Nyumba Kumi elder or 2 existing verified cluster neighbors.
2. **Data Privacy & Ephemerality:**
   - Real-time SOS locations expire after 2 hours.
   - House numbers and exact phone numbers remain masked behind in-app relay proxies unless explicitly permitted.
3. **Network Resiliency (Offline & Low Bandwidth):**
   - Offline caching for emergency contacts, gate telephone lines, and local elder numbers.
   - Highly compressed SVG and progressive WebP assets for smooth loading over 3G/4G connections.
4. **Payments & Compliance:**
   - Integrated with Safaricom Daraja M-Pesa API for instant, low-fee C2B and B2C Harambee pool contributions with transparent receipting.

---

## 6. Success Metrics & KPIs

- **Hyper-Local Density:** ≥ 65% of households verified in any onboarded 200m cluster radius within 30 days.
- **Mutual Aid Velocity:** > 120 completed non-monetary tool loans / carpool rides per cluster per quarter.
- **Baraza Quorum Attainment:** Increase average townhall participation from < 15% to > 60% through hybrid live-streaming.
- **Emergency Response Time:** Sub-3 minute acknowledgement time for urgent medical and security beacons.
