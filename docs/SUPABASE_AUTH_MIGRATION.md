# TRIMAX Supabase Auth migration plan

## Status
- Prepared an isolated branch: `feature/supabase-auth-migration`.
- This migration is additive and intended for a **staging Supabase project only**.
- No production database has been changed and no branches have been merged.

## Why this is additive
The Android application currently stores users and deliveries in local SQLite. The desktop application uses JDBC and a legacy PostgreSQL schema (`users`, `roles`, `clients`, `vehicles`, `bookings`, `booking_items`). The two booking models are not equivalent. This migration introduces a separate delivery-specific table rather than forcing delivery data into product stock/order rows.

## Target identity model
- Supabase Auth owns credentials, sessions, recovery, and UUID identity.
- `public.profiles.id` references `auth.users.id`.
- `profiles.role` is assigned only by a trusted admin process; clients must not be allowed to self-promote.
- `legacy_username` provides a controlled mapping to existing usernames during migration.
- Do not migrate plaintext passwords or assume the existing PBKDF2 format is compatible with Supabase Auth. Use verified email invitations or password reset for accounts that cannot be safely migrated.

## Phased rollout
1. **Inventory and backup**: export schema and data, capture current app behavior, create a separate staging Supabase project, and verify the current branch state.
2. **Identity mapping**: reconcile usernames, roles, agent/client relationships, driver records, and verified email addresses. Flag duplicates/missing emails for manual resolution.
3. **Staging schema**: apply `supabase/migrations/202610230001_supabase_auth_delivery_model.sql` to staging only. Review policies against the actual project and run SQL lint/tests.
4. **Auth proof of concept**: add Supabase Auth login/session refresh to a separate Android test build. Store only the session securely; never embed the service-role key.
5. **Desktop integration**: the existing desktop app connects with JDBC credentials. Do not expose database owner credentials in a distributed desktop app. Move privileged actions to a trusted API/service, or design a server-side Auth flow with tightly scoped permissions before using production.
6. **Booking workflow**: agent creates a PENDING row; admin assigns a driver and vehicle; driver reads assigned rows and updates an allowed status; notifications/realtime are added only after authorization is verified.
7. **Security testing**: verify anonymous access is denied, agent A cannot read agent B bookings, drivers cannot read other drivers' bookings or assign themselves, and only admins can assign drivers/change privileged fields.
8. **Pilot/cutover**: use test accounts and a small pilot, reconcile counts, keep the old system available during validation, and maintain a documented rollback path.
9. **Retirement**: disable legacy authentication only after all client flows, recovery, and role checks pass in production.

## Important security review items
- The SQL file is a starting point, not a claim that the project is production-ready. The driver update policy currently permits the assigned driver to update the row; before production, replace broad row updates with a secure RPC/function that accepts only allowed status transitions and cannot modify customer, price, assignment, or coordinates.
- The desktop JDBC path is not made safe by adding Supabase Auth to Android. A trusted backend is recommended for admin operations.
- Check current Supabase project schema, triggers, grants, and RLS before applying migrations.
- Back up first; never run this migration directly against production without review and explicit approval.

## Acceptance checklist
- [ ] Android auth uses Supabase Auth, no service-role key.
- [ ] Role is derived from trusted profile data and enforced by RLS.
- [ ] Agent booking appears in admin view.
- [ ] Admin assignment appears in driver view.
- [ ] Driver status change appears to authorized viewers.
- [ ] Cross-account access tests fail as expected.
- [ ] Rollback and recovery are tested.
