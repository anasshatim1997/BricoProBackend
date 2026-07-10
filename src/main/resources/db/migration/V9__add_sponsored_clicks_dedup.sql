CREATE TABLE sponsored_clicks (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    sponsored_worker_id  BIGINT NOT NULL,
    viewer_id            BIGINT NOT NULL,
    clicked_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sponsored_click UNIQUE (sponsored_worker_id, viewer_id),
    CONSTRAINT fk_sc_sponsored FOREIGN KEY (sponsored_worker_id) REFERENCES sponsored_workers(id),
    CONSTRAINT fk_sc_viewer FOREIGN KEY (viewer_id) REFERENCES users(id)
);
