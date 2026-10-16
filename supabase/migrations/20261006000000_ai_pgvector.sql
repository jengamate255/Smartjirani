-- Phase 1.1: Enable pgvector extension + add embedding columns for AI duplicate detection
-- Date: 2026-10-06
-- Purpose: Support semantic similarity search for smart duplicate detection

-- Enable pgvector extension
create extension if not exists vector with schema extensions;

-- Add embedding column to civic_reports for semantic search
-- Using 1536 dimensions (OpenAI ada-002 compatible, also works with Groq embeddings)
alter table civic_reports
  add column if not exists title_embedding vector(1536),
  add column if not exists description_embedding vector(1536),
  add column if not exists embedding_generated_at timestamptz;

-- Create index for fast similarity search (IVFFlat, 100 lists for ~10k-100k rows)
-- Note: Index will be populated after embeddings are generated
create index if not exists idx_civic_reports_title_embedding
  on civic_reports
  using ivfflat (title_embedding vector_cosine_ops)
  with (lists = 100);

create index if not exists idx_civic_reports_description_embedding
  on civic_reports
  using ivfflat (description_embedding vector_cosine_ops)
  with (lists = 100);

-- Function to find similar reports by embedding similarity
-- Returns reports with similarity > threshold (0.0 to 1.0, where 1.0 = identical)
create or replace function civic_find_similar_reports(
  p_report_id uuid,
  p_threshold float default 0.85,
  p_limit int default 10
)
returns table (
  report_id uuid,
  title text,
  similarity float,
  status text,
  category_id uuid,
  created_at timestamptz
)
language sql
stable
as $$
  with target as (
    select title_embedding, description_embedding
    from civic_reports
    where id = p_report_id
  )
  select
    r.id as report_id,
    r.title,
    1 - (r.title_embedding <=> t.title_embedding) as similarity,
    r.status::text,
    r.category_id,
    r.created_at
  from civic_reports r, target t
  where r.id != p_report_id
    and r.title_embedding is not null
    and 1 - (r.title_embedding <=> t.title_embedding) > p_threshold
  order by r.title_embedding <=> t.title_embedding
  limit p_limit;
$$;

-- Grant permissions
grant execute on function civic_find_similar_reports to authenticated;
grant execute on function civic_find_similar_reports to service_role;
