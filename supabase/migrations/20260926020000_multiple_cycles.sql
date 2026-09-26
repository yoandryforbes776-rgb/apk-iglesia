alter table public.ministry_records add column if not exists cycle_id text not null default 'default-cycle';
alter table public.ministry_records drop constraint if exists ministry_records_workspace_id_member_sync_id_meeting_id_key;
create unique index if not exists ministry_records_workspace_member_meeting_cycle
on public.ministry_records(workspace_id,member_sync_id,meeting_id,cycle_id);
create table if not exists public.ministry_cycles (
 workspace_id text not null,id text not null,church_id uuid not null references public.churches(id),name text not null,
 start_date text,end_date text,status text not null default 'ACTIVE',created_at bigint not null,updated_at bigint not null,
 primary key(workspace_id,id));
alter table public.ministry_cycles enable row level security;
create policy "church scoped cycles" on public.ministry_cycles for all to authenticated
using(public.has_church_access(church_id,false)) with check(public.has_church_access(church_id,true));
grant select,insert,update,delete on public.ministry_cycles to authenticated;
