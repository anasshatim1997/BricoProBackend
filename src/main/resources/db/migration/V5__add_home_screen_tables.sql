-- Service categories
CREATE TABLE service_category (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  `key` VARCHAR(50) NOT NULL UNIQUE,
                                  fr_name VARCHAR(100) NOT NULL,
                                  ar_name VARCHAR(100) NOT NULL,
                                  icon VARCHAR(10) NOT NULL,
                                  color VARCHAR(20) NOT NULL,
                                  price_min INT,
                                  price_max INT,
                                  display_order INT DEFAULT 0,
                                  active TINYINT(1) DEFAULT 1,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert initial services (matches your WorkerProfile.ServiceType enum)
INSERT INTO service_category (`key`, fr_name, ar_name, icon, color, price_min, price_max, display_order) VALUES
                                                                                                             ('REPAIRS', 'Réparations', 'إصلاحات', '🔧', '#E85D26', 150, 400, 1),
                                                                                                             ('ASSEMBLY', 'Montage', 'تركيب', '🪚', '#2E86AB', 100, 300, 2),
                                                                                                             ('MOVING', 'Déménagement', 'نقل', '🚚', '#6B4C3B', 300, 1500, 3),
                                                                                                             ('CLEANING', 'Nettoyage', 'تنظيف', '🧹', '#27AE60', 200, 600, 4),
                                                                                                             ('PAINTING', 'Peinture', 'دهان', '🎨', '#8E44AD', 400, 2000, 5),
                                                                                                             ('CONSTRUCTION', 'Assemblage', 'تجميع', '🛠️', '#F39C12', 200, 800, 6),
                                                                                                             ('OUTDOOR', 'Aide extérieure', 'مساعدة خارجية', '🌿', '#2ECC71', 150, 500, 7),
                                                                                                             ('DECORATION', 'Tendances', 'اتجاهات', '✨', '#E91E63', 300, 1500, 8),
                                                                                                             ('PLUMBING', 'Plomberie', 'سباكة', '🔧', '#1E88E5', 200, 500, 9);

-- Banners (admin managed)
CREATE TABLE banner (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        image_url VARCHAR(500) NOT NULL,
                        link_url VARCHAR(500),
                        start_date TIMESTAMP NOT NULL,
                        end_date TIMESTAMP NOT NULL,
                        active TINYINT(1) DEFAULT 1,
                        display_order INT DEFAULT 0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_banner_active_dates ON banner (active, start_date, end_date);
CREATE INDEX idx_service_category_active ON service_category (active, display_order);