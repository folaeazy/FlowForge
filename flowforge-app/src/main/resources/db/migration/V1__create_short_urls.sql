CREATE TABLE short_urls (
    id             BIGSERIAL PRIMARY KEY,
    short_code     VARCHAR(10) NOT NULL,
    long_url       TEXT NOT NULL,
    long_url_hash  CHAR(64) NOT NULL,
    tenant_id      VARCHAR(255) NOT NULL,
    job_id         VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT now()
);

-- Redirect lookups (GET /:shortCode) — the hottest read path
CREATE UNIQUE INDEX idx_short_urls_short_code ON short_urls (short_code);

-- Global idempotency check (same URL, any identity, returns same code)
CREATE UNIQUE INDEX idx_short_urls_long_url_hash ON short_urls (long_url_hash);

-- Dashboard's "recent URLs" panel, paginated by tenant
CREATE INDEX idx_short_urls_tenant_created ON short_urls (tenant_id, created_at DESC);