-- LogiVault V1 schema. See .agents/2-TECH-SPEC.md §2 for the full design rationale.
-- PostgreSQL 16. All primary keys are UUID; all timestamps are TIMESTAMPTZ (stored as UTC).

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL CHECK (role IN ('ADMIN','STAFF')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(64) NOT NULL UNIQUE,          -- SHA-256 hex of the raw token
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE login_attempts (
    email             VARCHAR(150) PRIMARY KEY,       -- lowercase
    failed_count      INT          NOT NULL DEFAULT 0,
    window_started_at TIMESTAMPTZ  NOT NULL,
    locked_until      TIMESTAMPTZ
);

CREATE TABLE items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150)  NOT NULL,
    description TEXT,
    base_price  NUMERIC(14,2) NOT NULL CHECK (base_price >= 0),
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_items_name ON items (lower(name));

CREATE TABLE variants (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id     UUID          NOT NULL REFERENCES items(id),
    sku         VARCHAR(64)   NOT NULL UNIQUE,         -- stored uppercase
    name        VARCHAR(100)  NOT NULL,
    attributes  JSONB         NOT NULL DEFAULT '{}'::jsonb,
    price       NUMERIC(14,2) CHECK (price >= 0),      -- NULL -> use items.base_price
    stock       INT           NOT NULL DEFAULT 0 CHECK (stock >= 0),
    min_stock   INT           NOT NULL DEFAULT 5 CHECK (min_stock >= 0),
    version     BIGINT        NOT NULL DEFAULT 0,
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_variants_item ON variants (item_id);

CREATE TABLE orders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(30)   NOT NULL UNIQUE,
    status        VARCHAR(10)   NOT NULL CHECK (status IN ('COMPLETED','CANCELLED')),
    total         NUMERIC(14,2) NOT NULL CHECK (total >= 0),
    note          VARCHAR(255),
    created_by    UUID          NOT NULL REFERENCES users(id),
    cancelled_by  UUID          REFERENCES users(id),
    cancelled_at  TIMESTAMPTZ,
    cancel_reason VARCHAR(255),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CHECK (status <> 'CANCELLED' OR (cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL AND cancel_reason IS NOT NULL))
);
CREATE INDEX ix_orders_status_created ON orders (status, created_at DESC);
CREATE INDEX ix_orders_created_by     ON orders (created_by);

CREATE TABLE order_items (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID          NOT NULL REFERENCES orders(id),
    variant_id UUID          NOT NULL REFERENCES variants(id),
    qty        INT           NOT NULL CHECK (qty > 0),
    unit_price NUMERIC(14,2) NOT NULL CHECK (unit_price >= 0),
    subtotal   NUMERIC(14,2) NOT NULL CHECK (subtotal >= 0),
    UNIQUE (order_id, variant_id)                       -- BR-13: one line per variant
);
CREATE INDEX ix_order_items_variant ON order_items (variant_id);

CREATE TABLE stock_movements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id   UUID         NOT NULL REFERENCES variants(id),
    type         VARCHAR(15)  NOT NULL CHECK (type IN ('STOCK_IN','ADJUSTMENT','SALE','SALE_CANCEL')),
    qty          INT          NOT NULL CHECK (qty <> 0),
    stock_before INT          NOT NULL CHECK (stock_before >= 0),
    stock_after  INT          NOT NULL CHECK (stock_after >= 0),
    reason       VARCHAR(255),
    order_id     UUID         REFERENCES orders(id),
    actor_id     UUID         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_movement_balance CHECK (stock_after = stock_before + qty),
    CONSTRAINT chk_movement_order_required CHECK (type NOT IN ('SALE','SALE_CANCEL') OR order_id IS NOT NULL),
    CONSTRAINT chk_movement_reason_required CHECK (type NOT IN ('ADJUSTMENT','SALE_CANCEL') OR reason IS NOT NULL)
);
CREATE INDEX ix_movements_variant_created ON stock_movements (variant_id, created_at DESC);
CREATE INDEX ix_movements_order           ON stock_movements (order_id);

CREATE TABLE order_code_counters (
    day      DATE PRIMARY KEY,
    last_seq INT  NOT NULL
);
