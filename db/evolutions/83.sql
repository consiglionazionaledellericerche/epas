# ---!Ups

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    version integer,
    message TEXT,
    reference TEXT,
    subject_id INT,
    subject TEXT,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

# ---!Downs

DROP TABLE notifications;