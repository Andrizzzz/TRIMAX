# Supabase Android rollout notes

## Client configuration
The Android client uses `SupabaseClient` with the project URL and publishable key. A publishable key is expected in client apps; it is not a privileged secret. Never add a `sb_secret_` or `service_role` key.

## Current implementation status
- Added a Java REST client for Supabase password sign-in and loading the authenticated user's `public.profiles` record.
- Login screen is intentionally not switched yet. Existing users currently authenticate against local SQLite and may have usernames or legacy password hashes that cannot be directly used with Supabase Auth.
- Booking submission is still local SQLite. Do not claim cross-device sync until booking insert/list/assignment/status flows are migrated and tested.
- Desktop administration still requires a trusted backend; do not ship PostgreSQL owner credentials inside the desktop client.

## Required setup before switching login
1. Apply `supabase/migrations/202610230001_supabase_auth_delivery_model.sql` to a TEST project first.
2. Create/invite each user in Supabase Auth using verified email ownership. Do not migrate plaintext passwords or trust user-editable metadata for roles.
3. For each verified Auth user, create a matching `public.profiles` row with the Auth UUID and admin-approved role. `id` must exactly match `auth.users.id`.
4. Test with separate Agent, Driver, and Admin accounts. Confirm each role sees only permitted rows under RLS.
5. Only then wire the login screen to Supabase Auth and persist access/refresh tokens securely. The current helper does not persist tokens or implement refresh/logout yet.
6. Implement booking create/list/admin assignment/driver status operations against `delivery_bookings`; remove the local driver-id fallback and ensure pending bookings have no assigned driver.
7. Move desktop privileged operations behind a trusted API before production. Do not connect the desktop app with a database-owner credential.

## Error handling
Supabase calls must run off the Android main thread. The helper currently does so with a single-thread executor. Review API responses and build/test on a real test project before release.
