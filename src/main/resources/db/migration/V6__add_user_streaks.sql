CREATE TABLE user_streaks (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              user_id BIGINT NOT NULL UNIQUE,
                              current_streak INT NOT NULL DEFAULT 0,
                              max_streak INT NOT NULL DEFAULT 0,
                              last_active_date DATE NOT NULL,
                              CONSTRAINT fk_streak_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);