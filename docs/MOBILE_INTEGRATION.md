# TRIMAX app ⇄ desktop: shared bookings

Branch `feature/mobile-booking-integration` (based on `redesign/corporate-ui-v2`, which includes the
PR #9 navigation fix). The database side lives in the desktop repository
(`GitBreakin/TrimaxSystem1`, same branch name): `sql/20261023_01_mobile_booking_integration.sql` and
`docs/MOBILE_INTEGRATION.md` (setup, end-to-end test, rollback). **The app needs that migration on the
Supabase project; without it every server call fails with a clear message.**

## How the app talks to the server

* Sign-in: Supabase Auth, email + password (`SupabaseClient`). The session is encrypted with an Android
  Keystore key (`AuthSessionStore`) and excluded from backups. Tokens refresh automatically.
* Data: only `POST /rest/v1/rpc/trimax_*` functions (`TrimaxApi`). The app never reads or writes tables.
  The server takes the user from the token, checks the TRIMAX role and Active status on every call, and
  returns only the caller's own bookings (agent) or assigned deliveries (driver).
* `SupabaseConfig` holds the project URL and the **publishable** key only. Never put a secret key,
  `service_role` key, database password or connection string in the app; `SupabaseClient` refuses them.

| Screen | Server function |
|---|---|
| Splash / Login | `trimax_my_account` |
| New booking (products, create) | `trimax_list_products`, `trimax_agent_create_booking` |
| Bookings, booking details, dashboard, reports, client history, tracking | `trimax_agent_list_bookings`, `trimax_agent_get_booking` |
| Driver dashboard, deliveries, details, map, history, proof | `trimax_driver_list_deliveries`, `trimax_driver_get_delivery`, `trimax_driver_update_delivery_status` |
| Change password | Supabase Auth `PUT /auth/v1/user` |
| Duty start/end with photo, live GPS | `trimax_record_duty_event`, `trimax_my_duty_status`, `trimax_update_location` |
| SOS and problem reports | `trimax_report_issue` |
| Drop-off photo | Storage bucket `delivery-proofs`, then `trimax_driver_attach_proof` |
| Price list pictures | public bucket `product-images` (after the Stage F database update) |

Driver delivery flow (same rules as the live server): **START DELIVERY** sends ASSIGNED -> STARTED (the
booking must be Confirmed by the administrator; it becomes In Progress). The drop-off button then opens the
proof screen, which sends STARTED -> DELIVERED with the recipient's name (the booking becomes Completed);
"Mark delivery failed" sends STARTED -> FAILED with a note. There are no intermediate statuses any more
(PICKED_UP / ON_THE_WAY / ARRIVED only exist on old rows, which can still be finished or failed), and
Start/End Duty never changes a delivery. A repeated request (double tap, retry after a timeout) is answered
with the saved booking, so it changes the status only once.

Still only on the phone (the app says so where relevant): saved-customer list, notifications, the local
driver location log, deposits, damage charges, profile edits.
OTP proof of delivery is not implemented; the recipient's name is required.

PR #8 (`feature/supabase-auth-migration`) is superseded: do **not** apply its `delivery_bookings` /
`profiles` migration. Its publishable key and project URL are reused here.

## Accounts

An agent or driver can sign in only after the administrator (1) creates a Supabase Auth user with the
same email as their TRIMAX account and (2) clicks **Link mobile sign-in** on the desktop. Agent and Rider
accounts get the agent screens; Driver accounts get the driver screens.

## Build and test (PowerShell)

```powershell
cd C:\Users\lance\AndroidStudioProjects\TRIMAX2
git status                                   # commit or stash your own changes first
git fetch origin
git checkout feature/mobile-booking-integration
.\gradlew.bat clean testDebugUnitTest assembleDebug
# APK: app\build\outputs\apk\debug\app-debug.apk
```

Unit tests (`app/src/test`): JSON parsing of real server responses, delivery-status rules, error mapping,
booking validation, filters, timestamps, and `SupabaseClient` against a local fake server (sign-in,
refresh, retry after 401, expired refresh token, typed errors, sign-out, unsafe config refused).
