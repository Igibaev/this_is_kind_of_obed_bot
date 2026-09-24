CREATE TABLE orders (
    id           BIGSERIAL PRIMARY KEY,
    chat_id      BIGINT NOT NULL REFERENCES users (chat_id),
    city         TEXT,
    status       TEXT,
    date         DATE NOT NULL,
    submitted_at TIMESTAMP,
    UNIQUE (chat_id, date)
);

CREATE TABLE order_items (
    id       BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    item_id  BIGINT REFERENCES menu_items (item_id) ON DELETE SET NULL,
    name     TEXT NOT NULL,
    category TEXT NOT NULL,
    UNIQUE (order_id, name)
);

CREATE TABLE order_categories (
    id       BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    UNIQUE (order_id, category)
);
