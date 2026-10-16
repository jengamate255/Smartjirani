-- ============================================================
-- 028: CIVIC NOTIFICATION TYPES
-- Extend notification_type enum with civic workflow values so the
-- civic RPCs can notify residents and officials without failing.
-- ============================================================

alter type public.notification_type add value if not exists 'CIVIC_STATUS_CHANGED' after 'GENERAL';
alter type public.notification_type add value if not exists 'CIVIC_NEW_REPORT' after 'CIVIC_STATUS_CHANGED';
alter type public.notification_type add value if not exists 'CIVIC_ASSIGNED' after 'CIVIC_NEW_REPORT';
alter type public.notification_type add value if not exists 'CIVIC_ESCALATED' after 'CIVIC_ASSIGNED';
alter type public.notification_type add value if not exists 'CIVIC_SLA_ALERT' after 'CIVIC_ESCALATED';
alter type public.notification_type add value if not exists 'CIVIC_ANNOUNCEMENT' after 'CIVIC_SLA_ALERT';

-- Unit-level routing without a named officer is a legitimate assignment
-- state (dept head routes to unit; unit officer picks a person later).
alter table public.civic_report_assignments alter column assigned_to drop not null;

-- The executing department closes out its own completed work; the seed
-- omitted DEPARTMENT_OFFICER from RESOLVED -> CLOSED by mistake.
update public.civic_transitions
set allowed_roles = array_append(allowed_roles, 'DEPARTMENT_OFFICER')
where actor_kind = 'OFFICIAL' and from_status = 'RESOLVED' and to_status = 'CLOSED';
