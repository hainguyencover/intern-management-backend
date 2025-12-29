CREATE TABLE IF NOT EXISTS intern_documents
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,

    intern_id
    BIGINT
    NOT
    NULL,
    type
    VARCHAR
(
    50
) NOT NULL,
    file_url VARCHAR
(
    500
) NOT NULL,
    status VARCHAR
(
    50
) NOT NULL DEFAULT 'PENDING',

    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    reviewed_by BIGINT NULL,
    reviewed_at DATETIME NULL,
    review_note VARCHAR
(
    1000
) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_intern_documents_intern
    FOREIGN KEY
(
    intern_id
) REFERENCES intern_profiles
(
    id
)
                                                           ON DELETE CASCADE,
    CONSTRAINT fk_intern_documents_reviewed_by
    FOREIGN KEY
(
    reviewed_by
) REFERENCES users
(
    id
)
                                                           ON DELETE SET NULL,

    -- 1 intern mỗi type chỉ 1 record active (nếu bạn muốn cho upload nhiều bản theo lịch sử thì bỏ unique này)
    CONSTRAINT uq_intern_documents_intern_type UNIQUE
(
    intern_id,
    type
)
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE =utf8mb4_unicode_ci;

CREATE INDEX idx_intern_documents_intern_id ON intern_documents (intern_id);
CREATE INDEX idx_intern_documents_status ON intern_documents (status);
CREATE INDEX idx_intern_documents_type ON intern_documents (type);
CREATE INDEX idx_intern_documents_intern_type ON intern_documents (intern_id, type);
