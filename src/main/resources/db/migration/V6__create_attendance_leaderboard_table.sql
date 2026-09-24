CREATE TABLE attendance_leaderboard (
    chat_id BIGINT NOT NULL REFERENCES users (chat_id),
    city TEXT NOT NULL,
    visits INT NOT NULL,
    PRIMARY KEY (chat_id, city)
);
