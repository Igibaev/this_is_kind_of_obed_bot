CREATE TABLE office_attendance (
    id TEXT PRIMARY KEY,
    chat_id TEXT NOT NULL,
    username TEXT,
    city TEXT,
    will_come BOOLEAN,
    date DATE NOT NULL
);
