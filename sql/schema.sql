CREATE TABLE IF NOT EXISTS tenders(
 id text PRIMARY KEY,
 reference text,
 title text NOT NULL,
 description text,
 buyer text,
 province text,
 category text,
 status text,
 published_at timestamptz,
 closing_at timestamptz,
 release_at timestamptz,
 source_url text NOT NULL,
 documents jsonb NOT NULL DEFAULT '[]'::jsonb,
 raw jsonb NOT NULL DEFAULT '{}'::jsonb,
 synced_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS tenders_closing_idx ON tenders(closing_at);
CREATE INDEX IF NOT EXISTS tenders_published_idx ON tenders(published_at DESC);
CREATE INDEX IF NOT EXISTS tenders_buyer_idx ON tenders(buyer);
CREATE INDEX IF NOT EXISTS tenders_province_idx ON tenders(province);
CREATE INDEX IF NOT EXISTS tenders_category_idx ON tenders(category);
CREATE INDEX IF NOT EXISTS tenders_text_search_idx ON tenders USING gin(to_tsvector('english',coalesce(title,'') || ' ' || coalesce(description,'') || ' ' || coalesce(reference,'')));
CREATE TABLE IF NOT EXISTS sync_runs(
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 started_at timestamptz NOT NULL DEFAULT now(),
 finished_at timestamptz,
 source text NOT NULL DEFAULT 'National Treasury OCDS',
 success boolean,
 pages integer NOT NULL DEFAULT 0,
 releases integer NOT NULL DEFAULT 0,
 error text
);
