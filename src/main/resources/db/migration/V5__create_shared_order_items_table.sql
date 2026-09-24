CREATE TABLE shared_order_items (
    id                  BIGSERIAL PRIMARY KEY,
    entry_id            TEXT NOT NULL UNIQUE,
    city                TEXT NOT NULL,
    date                DATE NOT NULL,
    item_id             BIGINT REFERENCES menu_items (item_id) ON DELETE SET NULL,
    name                TEXT NOT NULL,
    category            TEXT,
    source_chat_id      BIGINT REFERENCES users (chat_id),
    claimed_by_chat_id  BIGINT REFERENCES users (chat_id)
);

CREATE INDEX idx_shared_order_items_city_date ON shared_order_items (city, date);
