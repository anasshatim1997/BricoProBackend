CREATE TABLE users (
                       id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email        VARCHAR(255) UNIQUE,
                       phone        VARCHAR(20)  UNIQUE,
                       password_hash VARCHAR(255),
                       first_name   VARCHAR(100) NOT NULL,
                       last_name    VARCHAR(100) NOT NULL,
                       avatar_url   VARCHAR(500),
                       role         ENUM('CLIENT','WORKER','ADMIN') NOT NULL,
                       status       ENUM('PENDING','ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'PENDING',
                       is_verified  BOOLEAN NOT NULL DEFAULT FALSE,
                       is_online    BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE oauth2_accounts (
                                 id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id          BIGINT NOT NULL,
                                 provider         ENUM('GOOGLE','FACEBOOK') NOT NULL,
                                 provider_user_id VARCHAR(255) NOT NULL,
                                 access_token     TEXT,
                                 created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 UNIQUE KEY uq_provider_user (provider, provider_user_id),
                                 CONSTRAINT fk_oauth2_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
                                id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id    BIGINT       NOT NULL,
                                token      VARCHAR(512) NOT NULL UNIQUE,
                                expires_at DATETIME     NOT NULL,
                                revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
                                created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE otp_codes (
                           id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id    BIGINT      NOT NULL,
                           code       VARCHAR(10) NOT NULL,
                           purpose    ENUM('PHONE_VERIFY','PASSWORD_RESET') NOT NULL,
                           expires_at DATETIME    NOT NULL,
                           used       BOOLEAN     NOT NULL DEFAULT FALSE,
                           created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE worker_profiles (
                                 id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id               BIGINT         NOT NULL UNIQUE,
                                 bio                   TEXT,
                                 cin_document_url      VARCHAR(500),
                                 cin_verified          BOOLEAN        NOT NULL DEFAULT FALSE,
                                 average_rating        DECIMAL(3,2)   NOT NULL DEFAULT 0.00,
                                 total_reviews         INT            NOT NULL DEFAULT 0,
                                 total_missions        INT            NOT NULL DEFAULT 0,
                                 cancellation_count    INT            NOT NULL DEFAULT 0,
                                 response_rate         DECIMAL(5,2)   NOT NULL DEFAULT 100.00,
                                 intervention_radius_km INT           NOT NULL DEFAULT 20,
                                 city                  VARCHAR(100),
                                 latitude              DECIMAL(10,7),
                                 longitude             DECIMAL(10,7),
                                 bank_account          VARCHAR(100),
                                 is_premium            BOOLEAN        NOT NULL DEFAULT FALSE,
                                 CONSTRAINT fk_wp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE worker_services (
                                 id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 worker_id    BIGINT NOT NULL,
                                 service_type ENUM(
        'REPAIRS','ASSEMBLY','MOVING','CLEANING',
        'PAINTING','CONSTRUCTION','OUTDOOR','DECORATION','PLUMBING'
    ) NOT NULL,
                                 hourly_rate  DECIMAL(10,2),
                                 UNIQUE KEY uq_worker_service (worker_id, service_type),
                                 CONSTRAINT fk_ws_worker FOREIGN KEY (worker_id) REFERENCES worker_profiles(id) ON DELETE CASCADE
);

CREATE TABLE worker_availability (
                                     id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     worker_id  BIGINT NOT NULL,
                                     date       DATE   NOT NULL,
                                     status     ENUM('AVAILABLE','BUSY','LEAVE') NOT NULL DEFAULT 'AVAILABLE',
                                     start_time TIME,
                                     end_time   TIME,
                                     UNIQUE KEY uq_worker_date (worker_id, date),
                                     CONSTRAINT fk_wa_worker FOREIGN KEY (worker_id) REFERENCES worker_profiles(id) ON DELETE CASCADE
);

CREATE TABLE worker_portfolio (
                                  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  worker_id    BIGINT       NOT NULL,
                                  photo_url    VARCHAR(500) NOT NULL,
                                  caption      VARCHAR(255),
                                  service_type ENUM(
        'REPAIRS','ASSEMBLY','MOVING','CLEANING',
        'PAINTING','CONSTRUCTION','OUTDOOR','DECORATION','PLUMBING'
    ),
                                  photo_order  INT      NOT NULL DEFAULT 0,
                                  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_portfolio_worker FOREIGN KEY (worker_id) REFERENCES worker_profiles(id) ON DELETE CASCADE
);

CREATE TABLE worker_locations (
                                  worker_id       BIGINT PRIMARY KEY,
                                  latitude        DECIMAL(10,7),
                                  longitude       DECIMAL(10,7),
                                  speed_kmh       DOUBLE,
                                  heading_degrees DOUBLE,
                                  updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_loc_worker FOREIGN KEY (worker_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE worker_subscriptions (
                                      id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      worker_id          BIGINT NOT NULL,
                                      plan               ENUM('FREE','PREMIUM','ENTERPRISE') NOT NULL DEFAULT 'FREE',
                                      sub_status         ENUM('ACTIVE','EXPIRED','CANCELLED')  NOT NULL DEFAULT 'ACTIVE',
                                      started_at         DATETIME,
                                      expires_at         DATETIME,
                                      amount_paid        DECIMAL(10,2),
                                      payment_reference  VARCHAR(255),
                                      created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT fk_wsub_worker FOREIGN KEY (worker_id) REFERENCES users(id)
);

CREATE TABLE worker_badges (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id     BIGINT      NOT NULL,
                               badge_type  ENUM('NEW_WORKER','VERIFIED_CIN','TOP_RATED','EXPERIENCED','PREMIUM','FAST_RESPONDER','ZERO_CANCELLATIONS','ELITE') NOT NULL,
                               label       VARCHAR(100),
                               description VARCHAR(255),
                               icon_url    VARCHAR(50),
                               earned_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uq_badge (user_id, badge_type),
                               CONSTRAINT fk_badge_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE client_profiles (
                                 id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id           BIGINT       NOT NULL UNIQUE,
                                 company_name      VARCHAR(200),
                                 city              VARCHAR(100),
                                 default_address   TEXT,
                                 default_latitude  DECIMAL(10,7),
                                 default_longitude DECIMAL(10,7),
                                 CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE client_favorites (
                                  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  client_id  BIGINT   NOT NULL,
                                  worker_id  BIGINT   NOT NULL,
                                  saved_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  UNIQUE KEY uq_fav (client_id, worker_id),
                                  CONSTRAINT fk_fav_client FOREIGN KEY (client_id) REFERENCES users(id),
                                  CONSTRAINT fk_fav_worker FOREIGN KEY (worker_id) REFERENCES users(id)
);

CREATE TABLE tasks (
                       id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                       client_id           BIGINT       NOT NULL,
                       worker_id           BIGINT,
                       service_type        ENUM(
        'REPAIRS','ASSEMBLY','MOVING','CLEANING',
        'PAINTING','CONSTRUCTION','OUTDOOR','DECORATION','PLUMBING'
    ) NOT NULL,
                       title               VARCHAR(255) NOT NULL,
                       description         TEXT         NOT NULL,
                       address             TEXT         NOT NULL,
                       latitude            DECIMAL(10,7),
                       longitude           DECIMAL(10,7),
                       scheduled_date      DATE         NOT NULL,
                       scheduled_start     TIME         NOT NULL,
                       scheduled_end       TIME,
                       budget_min          DECIMAL(10,2),
                       budget_max          DECIMAL(10,2),
                       agreed_price        DECIMAL(10,2),
                       status              ENUM('PENDING','SEARCHING','CONFIRMED','STARTED','COMPLETED','CANCELLED','DISPUTED') NOT NULL DEFAULT 'PENDING',
                       is_urgent           BOOLEAN      NOT NULL DEFAULT FALSE,
                       cancelled_by        ENUM('CLIENT','WORKER','ADMIN'),
                       cancellation_reason TEXT,
                       created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT fk_task_client FOREIGN KEY (client_id) REFERENCES users(id),
                       CONSTRAINT fk_task_worker FOREIGN KEY (worker_id) REFERENCES users(id)
);

CREATE TABLE task_photos (
                             id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                             task_id     BIGINT       NOT NULL,
                             url         VARCHAR(500) NOT NULL,
                             uploaded_by BIGINT       NOT NULL,
                             created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_tp_task FOREIGN KEY (task_id)     REFERENCES tasks(id) ON DELETE CASCADE,
                             CONSTRAINT fk_tp_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE TABLE task_cancellation_log (
                                       id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       task_id             BIGINT   NOT NULL,
                                       cancelled_by_user_id BIGINT  NOT NULL,
                                       reason              TEXT,
                                       cancelled_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_cancel_task FOREIGN KEY (task_id)              REFERENCES tasks(id),
                                       CONSTRAINT fk_cancel_user FOREIGN KEY (cancelled_by_user_id) REFERENCES users(id)
);

CREATE TABLE reviews (
                         id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                         task_id     BIGINT   NOT NULL,
                         reviewer_id BIGINT   NOT NULL,
                         reviewee_id BIGINT   NOT NULL,
                         rating      TINYINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
                         comment     TEXT,
                         created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         UNIQUE KEY uq_review_task_reviewer (task_id, reviewer_id),
                         CONSTRAINT fk_rev_task     FOREIGN KEY (task_id)     REFERENCES tasks(id),
                         CONSTRAINT fk_rev_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
                         CONSTRAINT fk_rev_reviewee FOREIGN KEY (reviewee_id) REFERENCES users(id)
);

CREATE TABLE conversations (
                               id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                               task_id    BIGINT,
                               client_id  BIGINT   NOT NULL,
                               worker_id  BIGINT   NOT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uq_conv (client_id, worker_id, task_id),
                               CONSTRAINT fk_conv_task   FOREIGN KEY (task_id)   REFERENCES tasks(id),
                               CONSTRAINT fk_conv_client FOREIGN KEY (client_id) REFERENCES users(id),
                               CONSTRAINT fk_conv_worker FOREIGN KEY (worker_id) REFERENCES users(id)
);

CREATE TABLE messages (
                          id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                          conversation_id BIGINT       NOT NULL,
                          sender_id       BIGINT       NOT NULL,
                          content         TEXT,
                          media_url       VARCHAR(500),
                          message_type    ENUM('TEXT','IMAGE','CALL') NOT NULL DEFAULT 'TEXT',
                          is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
                          created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_msg_conv   FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
                          CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id)       REFERENCES users(id)
);

CREATE TABLE notifications (
                               id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id        BIGINT       NOT NULL,
                               type           ENUM(
        'NEW_TASK','TASK_ACCEPTED','TASK_STARTED','TASK_COMPLETED',
        'TASK_CANCELLED','NEW_MESSAGE','PAYMENT_RECEIVED',
        'REVIEW_RECEIVED','ACCOUNT_VERIFIED','SYSTEM'
    ) NOT NULL,
                               title          VARCHAR(255) NOT NULL,
                               body           TEXT         NOT NULL,
                               reference_id   BIGINT,
                               reference_type VARCHAR(50),
                               is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
                               created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE payments (
                          id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                          task_id           BIGINT       NOT NULL UNIQUE,
                          client_id         BIGINT       NOT NULL,
                          worker_id         BIGINT       NOT NULL,
                          gross_amount      DECIMAL(10,2) NOT NULL,
                          platform_fee      DECIMAL(10,2) NOT NULL,
                          processing_fee    DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                          net_amount        DECIMAL(10,2) NOT NULL,
                          currency          VARCHAR(3)   NOT NULL DEFAULT 'MAD',
                          method            ENUM('CMI','CIH_PAY','BANK_TRANSFER','CASH') NOT NULL,
                          status            ENUM('PENDING','PROCESSING','COMPLETED','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
                          gateway_reference VARCHAR(255),
                          paid_at           DATETIME,
                          created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT fk_pay_task   FOREIGN KEY (task_id)   REFERENCES tasks(id),
                          CONSTRAINT fk_pay_client FOREIGN KEY (client_id) REFERENCES users(id),
                          CONSTRAINT fk_pay_worker FOREIGN KEY (worker_id) REFERENCES users(id)
);

CREATE TABLE platform_revenue (
                                  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  payment_id BIGINT        NOT NULL UNIQUE,
                                  amount     DECIMAL(10,2) NOT NULL,
                                  month      INT           NOT NULL,
                                  year       INT           NOT NULL,
                                  created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_pr_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE TABLE group_bookings (
                                id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                                client_id         BIGINT       NOT NULL,
                                service_type      ENUM('REPAIRS','ASSEMBLY','MOVING','CLEANING','PAINTING','CONSTRUCTION','OUTDOOR','DECORATION','PLUMBING') NOT NULL,
                                title             VARCHAR(255) NOT NULL,
                                description       TEXT,
                                address           TEXT         NOT NULL,
                                scheduled_date    DATE         NOT NULL,
                                scheduled_start   TIME         NOT NULL,
                                workers_needed    INT          NOT NULL,
                                workers_confirmed INT          NOT NULL DEFAULT 0,
                                budget_per_worker DECIMAL(10,2),
                                status            ENUM('OPEN','PARTIAL','CONFIRMED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'OPEN',
                                created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_gb_client FOREIGN KEY (client_id) REFERENCES users(id)
);

CREATE TABLE group_booking_workers (
                                       id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       group_booking_id BIGINT   NOT NULL,
                                       worker_id        BIGINT   NOT NULL,
                                       task_id          BIGINT,
                                       joined_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       CONSTRAINT fk_gbw_booking FOREIGN KEY (group_booking_id) REFERENCES group_bookings(id),
                                       CONSTRAINT fk_gbw_worker  FOREIGN KEY (worker_id)        REFERENCES users(id)
);

CREATE TABLE recurring_tasks (
                                 id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 client_id           BIGINT       NOT NULL,
                                 preferred_worker_id BIGINT,
                                 service_type        ENUM('REPAIRS','ASSEMBLY','MOVING','CLEANING','PAINTING','CONSTRUCTION','OUTDOOR','DECORATION','PLUMBING') NOT NULL,
                                 title               VARCHAR(255) NOT NULL,
                                 description         TEXT,
                                 address             TEXT         NOT NULL,
                                 frequency           ENUM('DAILY','WEEKLY','BIWEEKLY','MONTHLY') NOT NULL,
                                 preferred_time      TIME,
                                 next_scheduled_date DATE,
                                 end_date            DATE,
                                 status              ENUM('ACTIVE','PAUSED','CANCELLED') NOT NULL DEFAULT 'ACTIVE',
                                 created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_rectask_client FOREIGN KEY (client_id)           REFERENCES users(id),
                                 CONSTRAINT fk_rectask_worker FOREIGN KEY (preferred_worker_id) REFERENCES users(id)
);

CREATE TABLE sponsored_workers (
                                   id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   worker_id     BIGINT NOT NULL,
                                   service_type  ENUM('REPAIRS','ASSEMBLY','MOVING','CLEANING','PAINTING','CONSTRUCTION','OUTDOOR','DECORATION','PLUMBING'),
                                   target_city   VARCHAR(100),
                                   daily_budget  DECIMAL(10,2),
                                   spent         DECIMAL(10,2) DEFAULT 0,
                                   cost_per_click DECIMAL(10,2),
                                   impressions   BIGINT        DEFAULT 0,
                                   clicks        BIGINT        DEFAULT 0,
                                   starts_at     DATETIME,
                                   ends_at       DATETIME,
                                   active        BOOLEAN       NOT NULL DEFAULT TRUE,
                                   created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_sp_worker FOREIGN KEY (worker_id) REFERENCES users(id)
);

CREATE TABLE device_tokens (
                               id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id      BIGINT       NOT NULL,
                               device_token VARCHAR(500) NOT NULL,
                               platform     ENUM('ANDROID','IOS') NOT NULL,
                               registered_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uq_device (user_id, device_token),
                               CONSTRAINT fk_dt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE referral_codes (
                                id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                                user_id              BIGINT      NOT NULL UNIQUE,
                                code                 VARCHAR(20) NOT NULL UNIQUE,
                                times_used           INT          NOT NULL DEFAULT 0,
                                total_rewards_earned DECIMAL(10,2) NOT NULL DEFAULT 0,
                                created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_rc_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE referral_uses (
                               id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                               referrer_id     BIGINT      NOT NULL,
                               referred_id     BIGINT      NOT NULL,
                               code            VARCHAR(20),
                               referrer_reward DECIMAL(10,2),
                               referred_reward DECIMAL(10,2),
                               reward_status   ENUM('PENDING','CREDITED','EXPIRED') NOT NULL DEFAULT 'PENDING',
                               used_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               credited_at     DATETIME,
                               CONSTRAINT fk_ru_referrer FOREIGN KEY (referrer_id) REFERENCES users(id),
                               CONSTRAINT fk_ru_referred FOREIGN KEY (referred_id) REFERENCES users(id)
);

CREATE TABLE worker_recommendations (
                                        id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        recommender_id BIGINT      NOT NULL,
                                        worker_id      BIGINT      NOT NULL,
                                        note           VARCHAR(500),
                                        created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        UNIQUE KEY uq_rec (recommender_id, worker_id),
                                        CONSTRAINT fk_rec_recommender FOREIGN KEY (recommender_id) REFERENCES users(id),
                                        CONSTRAINT fk_rec_worker      FOREIGN KEY (worker_id)      REFERENCES users(id)
);

CREATE TABLE user_preferences (
                                  user_id            BIGINT      PRIMARY KEY,
                                  language           VARCHAR(5)  NOT NULL DEFAULT 'fr',
                                  theme              VARCHAR(20) NOT NULL DEFAULT 'light',
                                  push_enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
                                  email_enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
                                  sms_enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
                                  marketing_enabled  BOOLEAN     NOT NULL DEFAULT FALSE,
                                  default_city       VARCHAR(100),
                                  default_latitude   DECIMAL(10,7),
                                  default_longitude  DECIMAL(10,7),
                                  updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_status          ON tasks(status);
CREATE INDEX idx_tasks_service_type    ON tasks(service_type);
CREATE INDEX idx_tasks_client          ON tasks(client_id);
CREATE INDEX idx_tasks_worker          ON tasks(worker_id);
CREATE INDEX idx_tasks_scheduled_date  ON tasks(scheduled_date);
CREATE INDEX idx_notifications_user    ON notifications(user_id, is_read);
CREATE INDEX idx_messages_conv         ON messages(conversation_id, created_at);
CREATE INDEX idx_refresh_tokens        ON refresh_tokens(token);
CREATE INDEX idx_worker_avail          ON worker_availability(worker_id, date);
CREATE INDEX idx_payments_status       ON payments(status);
CREATE INDEX idx_reviews_reviewee      ON reviews(reviewee_id);
CREATE INDEX idx_portfolio_worker      ON worker_portfolio(worker_id);
CREATE INDEX idx_sponsored_active      ON sponsored_workers(active, service_type, target_city);
CREATE INDEX idx_device_tokens_user    ON device_tokens(user_id);
CREATE INDEX idx_referral_codes_code   ON referral_codes(code);
CREATE INDEX idx_recurring_tasks_next  ON recurring_tasks(status, next_scheduled_date);