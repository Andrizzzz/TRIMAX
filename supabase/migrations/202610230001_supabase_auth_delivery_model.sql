-- TRIMAX Supabase Auth migration (additive / staging-first)
-- Apply to a TEST Supabase project first. This does not alter or delete legacy tables.
-- Do not put a service_role key in either client application.

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text unique,
    display_name text,
    role text not null check (role in ('ADMIN', 'AGENT', 'DRIVER')),
    legacy_username text unique,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.delivery_bookings (
    id uuid primary key default gen_random_uuid(),
    booking_code text not null unique,
    submitted_by uuid not null references public.profiles(id),
    agent_legacy_username text,
    customer_name text not null,
    customer_phone text not null,
    customer_email text,
    service_name text not null,
    quantity integer not null default 1 check (quantity > 0),
    unit_price numeric(12,2) not null default 0 check (unit_price >= 0),
    total_amount numeric(12,2) generated always as (quantity * unit_price) stored,
    pickup_address text not null,
    pickup_latitude double precision,
    pickup_longitude double precision,
    destination_address text not null,
    destination_latitude double precision not null check (destination_latitude between -90 and 90),
    destination_longitude double precision not null check (destination_longitude between -180 and 180),
    scheduled_at timestamptz,
    special_instructions text,
    assigned_driver_id uuid references public.profiles(id),
    assigned_driver_legacy_username text,
    vehicle_id text,
    status text not null default 'PENDING'
        check (status in ('PENDING','CONFIRMED','ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED','FAILED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_delivery_bookings_submitter
    on public.delivery_bookings(submitted_by, created_at desc);
create index if not exists idx_delivery_bookings_driver
    on public.delivery_bookings(assigned_driver_id, status, created_at desc);
create index if not exists idx_delivery_bookings_status
    on public.delivery_bookings(status, created_at desc);

create or replace function public.trimax_current_role()
returns text
language sql
stable
security definer
set search_path = ''
as $$
    select p.role
    from public.profiles p
    where p.id = (select auth.uid())
      and p.is_active = true
    limit 1
$$;

create or replace function public.trimax_touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_profiles_updated_at on public.profiles;
create trigger trg_profiles_updated_at before update on public.profiles
for each row execute function public.trimax_touch_updated_at();

drop trigger if exists trg_delivery_bookings_updated_at on public.delivery_bookings;
create trigger trg_delivery_bookings_updated_at before update on public.delivery_bookings
for each row execute function public.trimax_touch_updated_at();

alter table public.profiles enable row level security;
alter table public.delivery_bookings enable row level security;

-- Profiles are readable only by the owner and admins. Users cannot change their own role.
drop policy if exists profiles_read_self_or_admin on public.profiles;
create policy profiles_read_self_or_admin on public.profiles
for select to authenticated
using (id = (select auth.uid()) or public.trimax_current_role() = 'ADMIN');

drop policy if exists profiles_admin_manage on public.profiles;
create policy profiles_admin_manage on public.profiles
for all to authenticated
using (public.trimax_current_role() = 'ADMIN')
with check (public.trimax_current_role() = 'ADMIN');

-- Agents submit their own bookings. Agents see only their own; drivers see assigned
-- bookings; admins can review and assign any booking.
drop policy if exists delivery_booking_read_scoped on public.delivery_bookings;
create policy delivery_booking_read_scoped on public.delivery_bookings
for select to authenticated
using (
    public.trimax_current_role() = 'ADMIN'
    or submitted_by = (select auth.uid())
    or assigned_driver_id = (select auth.uid())
);

drop policy if exists delivery_booking_agent_insert on public.delivery_bookings;
create policy delivery_booking_agent_insert on public.delivery_bookings
for insert to authenticated
with check (
    public.trimax_current_role() = 'AGENT'
    and submitted_by = (select auth.uid())
    and assigned_driver_id is null
    and status = 'PENDING'
);

drop policy if exists delivery_booking_admin_update on public.delivery_bookings;
create policy delivery_booking_admin_update on public.delivery_bookings
for update to authenticated
using (public.trimax_current_role() = 'ADMIN')
with check (public.trimax_current_role() = 'ADMIN');

-- Assigned drivers can update status only on their own assigned bookings.
-- Production rollout should additionally restrict allowed status transitions via a
-- database function/RPC; do not grant driver-wide UPDATE permissions without testing.
drop policy if exists delivery_booking_driver_update on public.delivery_bookings;
create policy delivery_booking_driver_update on public.delivery_bookings
for update to authenticated
using (
    public.trimax_current_role() = 'DRIVER'
    and assigned_driver_id = (select auth.uid())
)
with check (
    public.trimax_current_role() = 'DRIVER'
    and assigned_driver_id = (select auth.uid())
);

grant usage on schema public to authenticated;
grant select on public.profiles to authenticated;
grant insert, select, update on public.delivery_bookings to authenticated;

-- IMPORTANT:
-- 1) Do not auto-create profiles from user-editable user_metadata; role assignment must be admin-controlled.
-- 2) Seed profiles only after verifying each auth.users identity and legacy account ownership.
-- 3) Review RLS with separate agent/driver/admin test accounts before production.
-- 4) Desktop JDBC currently uses direct PostgreSQL credentials. Keep it isolated until it
--    is moved behind a trusted service/API or a carefully secured server-side Auth flow.
