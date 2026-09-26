-- Impulso Joven · esquema seguro multi-iglesia
-- Ejecutar en Supabase Dashboard → SQL Editor. Es idempotente y conserva datos existentes.
create extension if not exists pgcrypto;

create table if not exists public.churches (
  id uuid primary key default gen_random_uuid(),
  workspace_code text not null unique,
  name text not null,
  created_at timestamptz not null default now()
);
create table if not exists public.church_memberships (
  church_id uuid not null references public.churches(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'leader' check (role in ('admin','director','leader','viewer')),
  created_at timestamptz not null default now(),
  primary key (church_id,user_id)
);

create table if not exists public.ministry_members (
  workspace_id text not null, sync_id text not null, church_id uuid references public.churches(id),
  full_name text not null, birth_date text, group_name text, active boolean not null default true,
  created_at bigint not null, updated_at bigint not null, primary key (workspace_id,sync_id)
);
create table if not exists public.ministry_records (
  workspace_id text not null, sync_id text not null, church_id uuid references public.churches(id),
  member_sync_id text not null, meeting_id integer not null check (meeting_id between 1 and 12),
  attended boolean not null, notes text not null default '', created_at bigint not null, updated_at bigint not null,
  rubric_version integer not null default 2, scores jsonb not null default '{}'::jsonb,
  penalties jsonb not null default '[]'::jsonb, primary key (workspace_id,sync_id),
  unique (workspace_id,member_sync_id,meeting_id)
);
create table if not exists public.ministry_deletions (
  workspace_id text not null, sync_id text not null, church_id uuid references public.churches(id),
  entity_type text not null check (entity_type in ('MEMBER','RECORD')), deleted_at bigint not null,
  primary key (workspace_id,sync_id)
);

-- Actualización no destructiva de instalaciones creadas con el esquema anterior.
alter table public.ministry_members add column if not exists church_id uuid references public.churches(id);
alter table public.ministry_records add column if not exists church_id uuid references public.churches(id);
alter table public.ministry_deletions add column if not exists church_id uuid references public.churches(id);
insert into public.churches(workspace_code,name)
select distinct lower(workspace_id), workspace_id from public.ministry_members where workspace_id<>''
on conflict (workspace_code) do nothing;
insert into public.churches(workspace_code,name)
select distinct lower(workspace_id), workspace_id from public.ministry_records where workspace_id<>''
on conflict (workspace_code) do nothing;
update public.ministry_members m set church_id=c.id from public.churches c where m.church_id is null and c.workspace_code=lower(m.workspace_id);
update public.ministry_records r set church_id=c.id from public.churches c where r.church_id is null and c.workspace_code=lower(r.workspace_id);
update public.ministry_deletions d set church_id=c.id from public.churches c where d.church_id is null and c.workspace_code=lower(d.workspace_id);

-- El primer usuario autenticado reclama la comunidad existente; luego solo miembros autorizados acceden.
create or replace function public.claim_church(workspace_code text, display_name text)
returns uuid language plpgsql security definer set search_path=public as $$
declare v_church uuid; v_members integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  select id into v_church from public.churches where churches.workspace_code=lower(trim($1));
  if v_church is null then
    insert into public.churches(workspace_code,name) values(lower(trim($1)),coalesce(nullif(trim($2),''),'Ministerio Juvenil')) returning id into v_church;
  end if;
  if exists(select 1 from public.church_memberships where church_id=v_church and user_id=auth.uid()) then return v_church; end if;
  select count(*) into v_members from public.church_memberships where church_id=v_church;
  if v_members=0 then
    insert into public.church_memberships(church_id,user_id,role) values(v_church,auth.uid(),'admin') on conflict do nothing;
    return v_church;
  end if;
  raise exception 'Esta comunidad ya tiene administrador. Solicita que te agregue como líder.';
end $$;
grant execute on function public.claim_church(text,text) to authenticated;

create or replace function public.has_church_access(target_church uuid, require_write boolean default false)
returns boolean language sql stable security definer set search_path=public as $$
  select exists(select 1 from public.church_memberships cm
    where cm.church_id=target_church and cm.user_id=auth.uid()
      and (not require_write or cm.role<>'viewer'));
$$;
grant execute on function public.has_church_access(uuid,boolean) to authenticated;

alter table public.churches enable row level security;
alter table public.church_memberships enable row level security;
alter table public.ministry_members enable row level security;
alter table public.ministry_records enable row level security;
alter table public.ministry_deletions enable row level security;

drop policy if exists "church member can view church" on public.churches;
create policy "church member can view church" on public.churches for select to authenticated using
(public.has_church_access(id,false));
drop policy if exists "member can view memberships" on public.church_memberships;
create policy "member can view memberships" on public.church_memberships for select to authenticated using
(public.has_church_access(church_id,false));

-- Sustituye políticas antiguas demasiado amplias.
drop policy if exists "authenticated ministry members" on public.ministry_members;
drop policy if exists "authenticated ministry records" on public.ministry_records;
drop policy if exists "authenticated ministry deletions" on public.ministry_deletions;
drop policy if exists "church scoped members" on public.ministry_members;
create policy "church scoped members" on public.ministry_members for all to authenticated
using(public.has_church_access(church_id,false))
with check(public.has_church_access(church_id,true));
drop policy if exists "church scoped records" on public.ministry_records;
create policy "church scoped records" on public.ministry_records for all to authenticated
using(public.has_church_access(church_id,false))
with check(public.has_church_access(church_id,true));
drop policy if exists "church scoped deletions" on public.ministry_deletions;
create policy "church scoped deletions" on public.ministry_deletions for all to authenticated
using(public.has_church_access(church_id,false))
with check(public.has_church_access(church_id,true));

grant select on public.churches,public.church_memberships to authenticated;
grant select,insert,update,delete on public.ministry_members,public.ministry_records,public.ministry_deletions to authenticated;

-- Ciclos múltiples (v1.5): conserva registros anteriores en el ciclo predeterminado.
alter table public.ministry_records add column if not exists cycle_id text not null default 'default-cycle';
alter table public.ministry_records drop constraint if exists ministry_records_workspace_id_member_sync_id_meeting_id_key;
create unique index if not exists ministry_records_workspace_member_meeting_cycle
on public.ministry_records(workspace_id,member_sync_id,meeting_id,cycle_id);

create table if not exists public.ministry_cycles (
  workspace_id text not null, id text not null, church_id uuid not null references public.churches(id),
  name text not null, start_date text, end_date text, status text not null default 'ACTIVE',
  created_at bigint not null, updated_at bigint not null, primary key(workspace_id,id)
);
alter table public.ministry_cycles enable row level security;
drop policy if exists "church scoped cycles" on public.ministry_cycles;
create policy "church scoped cycles" on public.ministry_cycles for all to authenticated
using(public.has_church_access(church_id,false)) with check(public.has_church_access(church_id,true));
grant select,insert,update,delete on public.ministry_cycles to authenticated;
