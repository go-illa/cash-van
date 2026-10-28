# Sales module: QA and pilot rollout

The Sales module (`com.illa.cashvan.sales`) brings the sales PWA into the Cash Van app. This page is for QA, the Android owner and ops running the pilot. The plan it follows is `sales-agent-PWA/docs/superpowers/specs/2026-10-27-sales-module-in-cashvan-android-plan.md`.

## 1. Build setup

| What | How |
|---|---|
| Backends | Staging build: Rails staging + Supabase staging (`hlctyyyhrdiredcsfioy`), the same as the staging PWA at sales-agent-pwa.pages.dev. Production build: Rails production + Supabase production (`hlzgbivgdcesiaymbijn`). Compare the app only with the PWA on the same environment |
| Staging build with the Sales role before the backend sends `roles` | `./gradlew installStagingDebug -PmockRoles=sales,fulfillment` then sign in again |
| Coverage map | Add `MAPS_API_KEY=...` to `local.properties` (or `-PmapsApiKey=...`). The key needs **Maps SDK for Android** enabled; without it the map is blank |
| Inspect network calls (staging) | Chucker notification shows both Rails and Supabase traffic |
| Analytics | Staging sends Sales events to the app's staging Mixpanel project; production sends them to the sales PWA project (EU). Every Sales event has `platform = android_native` |

Sign-in accepts a phone number or an agent code (e.g. `ILLA_001`). Agent codes only work once the backend accepts `sales_agent.login`.

## 2. Who sees what

| Roles from login | Lands on |
|---|---|
| none / `fulfillment` | Today's Cash Van screens, unchanged |
| `sales` | Sales module |
| `sales` + `fulfillment` | Module picker; the last choice is remembered; switch from Profile or المزيد |

A `sales` agent needs a linked Supabase `agents` row (via `sales_agents.agent_id`, else phone). Without one, a sales-only agent can't sign in; an inactive one (`status != 1`) sees "حسابك غير مفعل".

## 3. Feature checklist

Run each on a real phone against staging data.

**Presell (بري سيل tab)**
- [ ] Route list loads, sorts by distance once GPS is on, search and filter chips work, pull to refresh
- [ ] Telesales / app order badge and Fawry badge show on the right merchants
- [ ] Merchant sheet: call, directions, front-door code copy, visit history
- [ ] Start visit: revisit warning; more than 100 m away is refused with the distance; merchant without coordinates is allowed
- [ ] Fawry questionnaire appears after the location check for merchants with no device and no "wants one"; duplicate device number shows its error
- [ ] Visit without order: reasons list, visit recorded, route progress updates
- [ ] With order: vendors → products, stock badges, out-of-stock "طلب مؤجل", quantity capped at stock
- [ ] Checkout: cash / credit (date ≥ today) / Fawry (receipt image or PDF), deferred requests lane, success celebration and sound
- [ ] Price tier changed in admin mid-visit → checkout refuses once and reprices
- [ ] Off-route order (FAB), create merchant (retail and a tier request), order history and cancel within 4 hours

**Cash van (كاش فان tab)**
- [ ] Home: stock overview, recent orders open the order detail (deliver, print, void, edit)
- [ ] Merchant list: nearby and all-route modes, 200 m check, Fawry questionnaire, "visit only" with reason
- [ ] Products: recommended section, spend gauge, prices for the merchant's tier and governorate only
- [ ] Checkout: VAT, cash discount, rebate validation, cash / credit / Fawry with receipt
- [ ] Honeywell printing from order detail
- [ ] تقفيل الحساب اليومي: totals and stock match the plan; the PDF opens and shares

**KPIs and extras**
- [ ] KPI hub rings and commission card (hidden when the agent has no commission row); side by side with the PWA's `/kpi` for the same agent, every number matches, and the middle ring reads "التغطية" with visited/assigned
- [ ] Make a visit, come back to المؤشرات after a minute: the numbers update without restarting the app
- [ ] Visits week, coverage by route with comparison, conversion with top reason, monthly report
- [ ] Leaderboard tabs and "(أنت)" row
- [ ] Coverage map pins, legend filter, search, merchant card
- [ ] Nearby off-route: add an unassigned merchant; request a transfer (WhatsApp opens after the request is saved); history; pending and declined states

## 4. Weak and dropped network

| Scenario | Expected |
|---|---|
| Airplane mode on any screen | The app's no-internet overlay; retry recovers |
| Network drops while the presell order is submitting, then retry | One order only: the cart keeps its idempotency key until the order succeeds |
| Network drops while a deferred request is submitting, then retry | One request only (`request_id` is reused); result `idempotent_replay` |
| Network drops while a cash van order is submitting, then retry within 5 minutes | Rails rejects the repeat with the "wait 5 minutes" message; no duplicate |
| Slow network on the route, products or KPIs | Supabase table reads (GET) retry up to twice automatically; RPC and edge-function calls (inventory, KPIs, orders) don't. Either way: a loading state, then content or a retry button |
| App killed mid presell order | Reopening the merchant restores the cart and deferred basket (Room) |
| App killed mid cash van order | The cart is lost (kept in memory, as in the PWA) |

## 5. Analytics check

In Mixpanel (staging project for staging builds), filter `platform = android_native` and walk the PWA's funnels from `mixpanel_events.md`:

1. Login → order: `agent_sign_in` → `merchant_detail_sheet_opened` → `visit_setup_sheet_opened` → `visit_type_selected` → `order_placed`
2. Ad-hoc: `ad_hoc_search_opened` → `ad_hoc_search_query_typed` → `merchant_selected` → `order_placed`
3. Visit without order: `visit_type_selected` (without_order) → `no_order_reason_selected` → `visit_without_order_completed`
4. New merchant: `merchant_creation_started` → `merchant_created` / `merchant_creation_failed`
5. Deferred request: `deferred_request_item_added` → `checkout_sheet_opened` → `deferred_request_submit_succeeded`
6. Commission: `commission_progress_card_viewed` → `commission_unlock_celebration_shown`

Not instrumented yet: KPI detail-page pickers (`kpi_period_*`, `kpi_week_*`, `kpi_coverage_scope_*`), `payment_method_selected` / `continue_to_products`, `vendor_cart_banner_viewed`, `deferred_request_items_auto_removed`, the commission update popup and the `cv_invoice_*` events (order detail is the existing screen).

## 6. Pilot rollout

1. **Backend ready**: the login returns `roles`, `agent_id`, `sales_agent_id`; agent codes accepted; profile endpoint returns `roles`.
2. **QA pass** on staging with sections 3–5.
3. **Release build**: fix the release build first (see open questions), then distribute the production build.
4. **Pilot group**: give the `sales` role to 5–10 agents chosen by ops. Everyone else keeps today's experience; the role is the switch.
5. **Fallback**: the PWA stays live. An agent who gets stuck uses the PWA for the rest of the day and reports the screen and time.
6. **Watch daily**: Mixpanel `order_placed` and `order_submission_failed` by `platform`; Supabase orders per pilot agent compared with their PWA baseline; Crashlytics.
7. **Rollback**: remove the `sales` role from the agent. They lose the Sales module at their next sign-in, or at app start once the profile endpoint returns roles. Sales-only pilot agents go back to the PWA.
8. **Exit**: a pilot agent completes full presell and cash van days with no PWA use for a week → widen the group → retire the PWA.

## 7. Open questions before the pilot

- **Release build**: release is `isDebuggable = true` and signed with the debug key. Switching to a real key changes the signature, so already-installed builds would need a reinstall; decide when.
- **Presell-only agents** have no Rails identity: backfill `sales_agents` or authenticate against `agents` in Rails.
- **Reconciliation** counts cash van orders only when `fulfilled`, but delivering sets `submitted` — confirm with the backend (same in the PWA).
- **Transfer requests** record the merchant's current route as "from"; the PWA records the agent's first route.
- **PWA freeze**: from now on, either every PWA ticket gets a native twin or the PWA takes bug fixes only.
