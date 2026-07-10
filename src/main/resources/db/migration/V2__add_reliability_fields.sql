ALTER TABLE users
    ADD COLUMN cancellation_count_this_month INT NOT NULL DEFAULT 0,
    ADD COLUMN reliability_score INT NOT NULL DEFAULT 100;

ALTER TABLE worker_profiles
    ADD COLUMN cancellation_count_this_month INT NOT NULL DEFAULT 0,
    ADD COLUMN total_cancellations_lifetime INT NOT NULL DEFAULT 0,
    ADD COLUMN reliability_score INT NOT NULL DEFAULT 100,
    ADD COLUMN visibility_reduction_until DATETIME,
    ADD COLUMN verified_badge BOOLEAN NOT NULL DEFAULT TRUE;

-- Optional indexes for performance (if you query by these fields)
CREATE INDEX idx_users_reliability ON users(reliability_score);
CREATE INDEX idx_worker_profiles_reliability ON worker_profiles(reliability_score);
CREATE INDEX idx_worker_profiles_visibility ON worker_profiles(visibility_reduction_until);