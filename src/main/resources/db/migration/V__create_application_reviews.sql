CREATE TABLE IF NOT EXISTS application_reviews (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   application_id BIGINT NOT NULL,
                                                   reviewer_id BIGINT NOT NULL,
                                                   decision VARCHAR(20) NOT NULL,
    comment TEXT NULL,
    decided_at DATETIME NOT NULL,
    CONSTRAINT fk_review_app FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
    );

CREATE INDEX idx_review_app_decided ON application_reviews(application_id, decided_at);
