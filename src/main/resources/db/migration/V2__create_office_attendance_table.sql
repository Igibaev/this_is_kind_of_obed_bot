CREATE TABLE office_attendance (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL REFERENCES users (chat_id),
    city TEXT,
    will_come BOOLEAN,
    date DATE NOT NULL,
    UNIQUE (chat_id, date)
);
