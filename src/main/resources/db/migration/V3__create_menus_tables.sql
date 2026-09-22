CREATE TABLE menus (
    id          BIGSERIAL PRIMARY KEY,
    city        TEXT NOT NULL,
    date        DATE NOT NULL,
    status      TEXT,
    deadline    TIMESTAMP,
    available   BOOLEAN,
    notificated BOOLEAN,
    message     TEXT,
    UNIQUE (city, date)
);

CREATE TABLE menu_items (
    item_id       BIGSERIAL PRIMARY KEY,
    menu_id       BIGINT NOT NULL REFERENCES menus (id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    name          TEXT NOT NULL,
    category      TEXT NOT NULL,
    UNIQUE (menu_id, display_order)
);
