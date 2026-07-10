CREATE TABLE IF NOT EXISTS bid (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   task_id BIGINT NOT NULL,
                                   worker_id BIGINT NOT NULL,
                                   amount DECIMAL(10,2) NOT NULL,
    message TEXT,
    estimated_duration_hours INT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    CONSTRAINT fk_bid_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_bid_worker FOREIGN KEY (worker_id) REFERENCES users(id) ON DELETE CASCADE
    );

CREATE INDEX idx_bid_task_status ON bid(task_id, status);
CREATE INDEX idx_bid_worker_status ON bid(worker_id, status);

ALTER TABLE tasks ADD COLUMN bidding_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE tasks ADD COLUMN bidding_deadline TIMESTAMP;
ALTER TABLE tasks ADD COLUMN auto_assign_enabled BOOLEAN DEFAULT FALSE;