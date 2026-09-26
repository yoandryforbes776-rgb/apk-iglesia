-- Impulso Joven · esquema de sincronización Supabase
-- Ejecutar una vez en Supabase Dashboard → SQL Editor.

create table if not exists public.ministry_members (
  workspace_id text not null,
  sync_id text not null,
  full_name text not null,
  birth_date text,
  group_name text,
  active boolean not null default true,
  created_at bigint not null,
  updated_at bigint not null,
  primary key (workspace_id, sync_id)
);

create table if not exists public.ministry_records (
  workspace_id text not null,
  sync_id text not null,
  member_sync_id text not null,
  meeting_id integer not null check (meeting_id between 1 and 12),
  attended boolean not null,
  notes text not null default '',
  created_at bigint not null,
  updated_at bigint not null,
  rubric_version integer not null default 2,
  scores jsonb not null default '{}'::jsonb,
  penalties jsonb not null default '[]'::jsonb,
  primary key (workspace_id, sync_id),
  unique (workspace_id, member_sync_id, meeting_id)
);

create table if not exists public.ministry_deletions (
  workspace_id text not null,
  sync_id text not null,
  entity_type text not null check (entity_type in ('MEMBER','RECORD')),
  deleted_at bigint not null,
  primary key (workspace_id, sync_id)
);

alter table public.ministry_members enable row level security;
alter table public.ministry_records enable row level security;
alter table public.ministry_deletions enable row level security;

-- La app exige iniciar sesión con un usuario de Supabase Auth.
-- Se recomienda usar un proyecto Supabase exclusivo para cada iglesia.
drop policy if exists "authenticated ministry members" on public.ministry_members;
create policy "authenticated ministry members" on public.ministry_members
  for all to authenticated using (true) with check (true);

drop policy if exists "authenticated ministry records" on public.ministry_records;
create policy "authenticated ministry records" on public.ministry_records
  for all to authenticated using (true) with check (true);

drop policy if exists "authenticated ministry deletions" on public.ministry_deletions;
create policy "authenticated ministry deletions" on public.ministry_deletions
  for all to authenticated using (true) with check (true);

grant select, insert, update, delete on public.ministry_members to authenticated;
grant select, insert, update, delete on public.ministry_records to authenticated;
grant select, insert, update, delete on public.ministry_deletions to authenticated;
