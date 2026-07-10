CREATE TABLE worker_snapshot_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_id           BIGINT NOT NULL,
    task_id             BIGINT NOT NULL,
    average_rating      DECIMAL(3,2) NOT NULL,
    total_reviews       INT NOT NULL,
    total_missions      INT NOT NULL,
    response_rate       DECIMAL(5,2) NOT NULL,
    cancellation_count  INT NOT NULL,
    is_premium          BOOLEAN NOT NULL,
    snapshotted_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wsh_worker FOREIGN KEY (worker_id) REFERENCES users(id),
    CONSTRAINT fk_wsh_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    INDEX idx_wsh_worker (worker_id),
    INDEX idx_wsh_task (task_id)
);
