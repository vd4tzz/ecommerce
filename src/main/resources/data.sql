-- =========================================================================
-- SEED DATA CHO DỰ ÁN ECOMMERCE ATELIER
-- =========================================================================

-- 1. Tài khoản Quản trị viên (Email: admin@example.com, Mật khẩu: Admin12345)
INSERT INTO users (id, email, password, full_name, role, status, email_verified, created_at, updated_at)
VALUES (nextval('users_seq'), 'admin@example.com', '$2a$10$lQOYp5ILEbbTn64bTd78k.TLwyTM9qURRKbNkad8IY.hZjlsUJcS.',
        'Administrator', 'ROLE_ADMIN', 'ACTIVE', true, now(), now())
ON CONFLICT (email) DO NOTHING;

-- Tài khoản Khách hàng mẫu (Email: customer@example.com, Mật khẩu: Customer12345)
INSERT INTO users (id, email, password, full_name, role, status, email_verified, created_at, updated_at)
VALUES (nextval('users_seq'), 'customer@example.com', '$2a$10$xSKiOcFf9QwT9WSKVhKv9eIq82RRkCywCl4MPEraX4fRI99oDKBjy',
        'Nguyễn Văn An', 'ROLE_CUSTOMER', 'ACTIVE', true, now(), now())
ON CONFLICT (email) DO NOTHING;

-- 2. Cây danh mục (Categories)
-- Danh mục gốc (Root: path = null, parent_id = null)
INSERT INTO categories (id, name, slug, path, parent_id, attributes) VALUES
(1, 'Trang phục Nam', 'trang-phuc-nam', NULL, NULL, '[]'::jsonb),
(2, 'Trang phục Nữ', 'trang-phuc-nu', NULL, NULL, '[]'::jsonb),
(3, 'Đời sống & Phụ kiện', 'doi-song-phu-kien', NULL, NULL, '[]'::jsonb)
ON CONFLICT (slug) DO NOTHING;

-- Danh mục con (Sub-categories)
INSERT INTO categories (id, name, slug, path, parent_id, attributes) VALUES
(11, 'Áo sơ mi Nam', 'ao-so-mi-nam', '1.', 1, '[]'::jsonb),
(12, 'Áo thun & Polo', 'ao-thun-polo', '1.', 1, '[]'::jsonb),
(13, 'Quần Nam Tối Giản', 'quan-nam-toi-gian', '1.', 1, '[]'::jsonb),
(21, 'Đầm & Chân váy', 'dam-chan-vay', '2.', 2, '[]'::jsonb),
(22, 'Áo Len & Knitwear', 'ao-len-knitwear', '2.', 2, '[]'::jsonb),
(31, 'Túi Canvas & Da', 'tui-canvas-da', '3.', 3, '[]'::jsonb),
(32, 'Đồ dùng Tối giản', 'do-dung-toi-gian', '3.', 3, '[]'::jsonb)
ON CONFLICT (slug) DO NOTHING;

-- 3. Tùy chọn sản phẩm (Options & Option Values)
INSERT INTO options (id, name) VALUES
(1, 'Màu sắc'),
(2, 'Kích cỡ')
ON CONFLICT (name) DO NOTHING;

INSERT INTO option_values (id, name, option_id) VALUES
(1, 'Trắng Ngà', 1),
(2, 'Đen Than', 1),
(3, 'Xanh Rêu', 1),
(4, 'Be Sáng', 1),
(11, 'S', 2),
(12, 'M', 2),
(13, 'L', 2),
(14, 'XL', 2)
ON CONFLICT (option_id, name) DO NOTHING;

-- 4. Danh sách Sản phẩm mẫu (Products)
INSERT INTO products (id, name, slug, description, category_id, status, min_price, max_price, attributes) VALUES
(
  1,
  'Áo Sơ Mi Linen Cổ Trụ Thụy Điển',
  'ao-so-mi-linen-co-tru-thuy-dien',
  'Áo sơ mi cổ trụ dệt từ 100% sợi lanh tự nhiên nhập khẩu từ vùng Bắc Âu. Chất vải thoáng mát, tạo nếp nhăn tự nhiên mộc mạc và sang trọng. Phom áo suông hiện đại tôn dáng nhưng vẫn mang lại cảm giác dễ chịu tuyệt đối.',
  11,
  'PUBLISHED',
  650000,
  690000,
  '{"material": ["100% Linen hữu cơ"], "fit": ["Regular Fit"], "origin": ["Việt Nam"], "season": ["Xuân Hè"]}'::jsonb
),
(
  2,
  'Áo Thun Heavyweight Organic Cotton',
  'ao-thun-heavyweight-organic-cotton',
  'Áo thun định lượng cao 260 GSM từ bông hữu cơ dày dặn, không bai dão sau nhiều lần giặt. Thiết kế cổ tròn may bo dày dặn, vai trễ nhẹ theo xu hướng tối giản Scandinavian.',
  12,
  'PUBLISHED',
  380000,
  380000,
  '{"material": ["100% Cotton 260GSM"], "fit": ["Relaxed Oversized"], "origin": ["Việt Nam"]}'::jsonb
),
(
  3,
  'Quần Tây Ống Rộng Minimalist Pleated',
  'quan-tay-ong-rong-minimalist-pleated',
  'Quần tây xếp ly đôi ống suông rộng thời thượng. Chất liệu len pha wool cao cấp giữ nếp ly sắc nét, có độ rũ mềm mại tự nhiên khi di chuyển.',
  13,
  'PUBLISHED',
  790000,
  850000,
  '{"material": ["Len pha Wool"], "fit": ["Wide Leg"], "origin": ["Việt Nam"]}'::jsonb
),
(
  4,
  'Đầm Suông Linen Cổ Vuông Scandinavian',
  'dam-suong-linen-co-vuong-scandinavian',
  'Đầm dài dáng suông thiết kế cổ vuông thanh lịch, may hai lớp từ vải linen thoáng mát. Gam màu trung tính nhã nhặn phù hợp cho cả đi làm lẫn dạo phố.',
  21,
  'PUBLISHED',
  820000,
  820000,
  '{"material": ["100% Linen hữu cơ"], "fit": ["Loose Fit"], "origin": ["Việt Nam"]}'::jsonb
),
(
  5,
  'Áo Len Dệt Kim Soft Touch Knitwear',
  'ao-len-det-kim-soft-touch-knitwear',
  'Áo len chui đầu dệt từ sợi len mềm mịn không gây ngứa dính da. Cổ tròn kinh điển cùng đường may tinh xảo ở viền gấu và cổ tay.',
  22,
  'PUBLISHED',
  890000,
  890000,
  '{"material": ["Len Merino dệt kim"], "fit": ["Regular Fit"], "origin": ["Việt Nam"]}'::jsonb
),
(
  6,
  'Chân Váy Xếp Ly Dáng Dài Tối Giản',
  'chan-vay-xep-ly-dang-dai-toi-gian',
  'Chân váy maxi xếp ly nhỏ tinh tế, cạp chun êm ái co giãn phía sau. Tạo phom bồng bềnh thanh thoát theo từng bước chân.',
  21,
  'PUBLISHED',
  580000,
  580000,
  '{"material": ["Polyester tái chế & Viscose"], "fit": ["Maxi A-Line"], "origin": ["Việt Nam"]}'::jsonb
),
(
  7,
  'Túi Tote Canvas Bắc Âu Quai Da Thật',
  'tui-tote-canvas-bac-au-quai-da-that',
  'Túi tote vải bố cotton tự nhiên mộc mạc kết hợp quai xách bằng da bò thật nguyên tấm dập đinh tán đồng. Đựng vừa laptop 15 inch và nhiều vật dụng cá nhân.',
  31,
  'PUBLISHED',
  420000,
  420000,
  '{"material": ["Vải bố Canvas 16oz & Da thật"], "capacity": ["15 lít"], "origin": ["Việt Nam"]}'::jsonb
),
(
  8,
  'Bình Giữ Nhiệt Matte Minimalist 600ml',
  'binh-giu-nhiet-matte-minimalist-600ml',
  'Bình giữ nhiệt chất liệu thép không gỉ 304 hai lớp cách nhiệt chân không, phủ sơn tĩnh điện nhám mờ tinh tế. Giữ nóng 12 giờ và giữ lạnh 24 giờ.',
  32,
  'PUBLISHED',
  320000,
  350000,
  '{"material": ["Inox 304 & Nắp silicon"], "capacity": ["600ml"], "origin": ["Việt Nam"]}'::jsonb
)
ON CONFLICT (slug) DO NOTHING;

-- 5. Thư viện Ảnh sản phẩm (Product Media)
INSERT INTO product_media (media_id, product_id, position, version) VALUES
('https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=1000&auto=format&fit=crop&q=80', 1, 0, 0),
('https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=1000&auto=format&fit=crop&q=80', 1, 1, 0),

('https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=1000&auto=format&fit=crop&q=80', 2, 0, 0),
('https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=1000&auto=format&fit=crop&q=80', 2, 1, 0),

('https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=1000&auto=format&fit=crop&q=80', 3, 0, 0),
('https://images.unsplash.com/photo-1506630448388-4e683c67ddb0?w=1000&auto=format&fit=crop&q=80', 3, 1, 0),

('https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=1000&auto=format&fit=crop&q=80', 4, 0, 0),
('https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=1000&auto=format&fit=crop&q=80', 4, 1, 0),

('https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=1000&auto=format&fit=crop&q=80', 5, 0, 0),
('https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=1000&auto=format&fit=crop&q=80', 5, 1, 0),

('https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=1000&auto=format&fit=crop&q=80', 6, 0, 0),

('https://images.unsplash.com/photo-1544816155-12df9643f363?w=1000&auto=format&fit=crop&q=80', 7, 0, 0),

('https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=1000&auto=format&fit=crop&q=80', 8, 0, 0)
ON CONFLICT (media_id) DO NOTHING;

-- 6. Gán Tùy chọn vào Sản phẩm (Product Options & Values)
-- Sản phẩm 1: Màu sắc (Trắng Ngà, Đen Than) & Kích cỡ (M, L)
INSERT INTO product_options (product_id, option_id, position, version) VALUES
(1, 1, 0, 0), (1, 2, 1, 0),
(2, 1, 0, 0), (2, 2, 1, 0),
(3, 1, 0, 0), (3, 2, 1, 0),
(4, 1, 0, 0), (4, 2, 1, 0),
(5, 1, 0, 0), (5, 2, 1, 0),
(6, 1, 0, 0), (6, 2, 1, 0),
(7, 1, 0, 0),
(8, 1, 0, 0)
ON CONFLICT DO NOTHING;

INSERT INTO product_option_values (product_id, option_id, value_id, position, version) VALUES
-- SP 1: Màu Trắng Ngà (1), Đen Than (2); Size M (12), L (13)
(1, 1, 1, 0, 0), (1, 1, 2, 1, 0), (1, 2, 12, 0, 0), (1, 2, 13, 1, 0),
-- SP 2: Màu Trắng Ngà (1), Đen Than (2), Xanh Rêu (3); Size M (12), L (13)
(2, 1, 1, 0, 0), (2, 1, 2, 1, 0), (2, 1, 3, 2, 0), (2, 2, 12, 0, 0), (2, 2, 13, 1, 0),
-- SP 3: Màu Đen Than (2), Be Sáng (4); Size M (12), L (13)
(3, 1, 2, 0, 0), (3, 1, 4, 1, 0), (3, 2, 12, 0, 0), (3, 2, 13, 1, 0),
-- SP 4: Màu Trắng Ngà (1), Be Sáng (4); Size S (11), M (12)
(4, 1, 1, 0, 0), (4, 1, 4, 1, 0), (4, 2, 11, 0, 0), (4, 2, 12, 1, 0),
-- SP 5: Màu Be Sáng (4), Đen Than (2); Size S (11), M (12)
(5, 1, 4, 0, 0), (5, 1, 2, 1, 0), (5, 2, 11, 0, 0), (5, 2, 12, 1, 0),
-- SP 6: Màu Đen Than (2), Be Sáng (4); Size S (11), M (12)
(6, 1, 2, 0, 0), (6, 1, 4, 1, 0), (6, 2, 11, 0, 0), (6, 2, 12, 1, 0),
-- SP 7: Màu Trắng Ngà (1), Đen Than (2)
(7, 1, 1, 0, 0), (7, 1, 2, 1, 0),
-- SP 8: Màu Đen Than (2), Trắng Ngà (1)
(8, 1, 2, 0, 0), (8, 1, 1, 1, 0)
ON CONFLICT DO NOTHING;

-- 7. Các Biến thể có thể mua được (Variants)
INSERT INTO variants (id, product_id, sku, price, compare_at, media_id, length, width, height, weight) VALUES
-- SP 1: Sơ mi Linen
(101, 1, 'LINEN-SHIRT-WHT-M', 650000, 750000, 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=1000&auto=format&fit=crop&q=80', 70, 50, 2, 250),
(102, 1, 'LINEN-SHIRT-WHT-L', 650000, 750000, 'https://images.unsplash.com/photo-1596755094514-f87e34085b2c?w=1000&auto=format&fit=crop&q=80', 72, 52, 2, 260),
(103, 1, 'LINEN-SHIRT-BLK-M', 690000, 790000, 'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=1000&auto=format&fit=crop&q=80', 70, 50, 2, 250),
(104, 1, 'LINEN-SHIRT-BLK-L', 690000, 790000, 'https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=1000&auto=format&fit=crop&q=80', 72, 52, 2, 260),

-- SP 2: Áo Thun Heavyweight
(201, 2, 'TSHIRT-ORG-WHT-M', 380000, 450000, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=1000&auto=format&fit=crop&q=80', 70, 52, 2, 260),
(202, 2, 'TSHIRT-ORG-WHT-L', 380000, 450000, 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=1000&auto=format&fit=crop&q=80', 72, 54, 2, 270),
(203, 2, 'TSHIRT-ORG-BLK-M', 380000, 450000, 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=1000&auto=format&fit=crop&q=80', 70, 52, 2, 260),
(204, 2, 'TSHIRT-ORG-SAGE-M', 380000, 450000, 'https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=1000&auto=format&fit=crop&q=80', 70, 52, 2, 260),

-- SP 3: Quần Tây Minimalist
(301, 3, 'PANTS-MIN-BLK-M', 790000, 890000, 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=1000&auto=format&fit=crop&q=80', 100, 40, 3, 400),
(302, 3, 'PANTS-MIN-BLK-L', 790000, 890000, 'https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=1000&auto=format&fit=crop&q=80', 102, 42, 3, 410),
(303, 3, 'PANTS-MIN-BEI-M', 850000, 950000, 'https://images.unsplash.com/photo-1506630448388-4e683c67ddb0?w=1000&auto=format&fit=crop&q=80', 100, 40, 3, 400),

-- SP 4: Đầm Suông Linen
(401, 4, 'DRESS-LIN-WHT-S', 820000, 950000, 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=1000&auto=format&fit=crop&q=80', 110, 45, 2, 350),
(402, 4, 'DRESS-LIN-WHT-M', 820000, 950000, 'https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=1000&auto=format&fit=crop&q=80', 112, 47, 2, 360),
(403, 4, 'DRESS-LIN-BEI-M', 820000, 950000, 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=1000&auto=format&fit=crop&q=80', 112, 47, 2, 360),

-- SP 5: Áo Len Knitwear
(501, 5, 'KNIT-MERINO-BEI-M', 890000, 1050000, 'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=1000&auto=format&fit=crop&q=80', 68, 50, 4, 450),
(502, 5, 'KNIT-MERINO-BLK-M', 890000, 1050000, 'https://images.unsplash.com/photo-1434389677669-e08b4cac3105?w=1000&auto=format&fit=crop&q=80', 68, 50, 4, 450),

-- SP 6: Chân Váy Maxi
(601, 6, 'SKIRT-MAXI-BLK-M', 580000, 680000, 'https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=1000&auto=format&fit=crop&q=80', 85, 36, 2, 300),

-- SP 7: Túi Tote Canvas
(701, 7, 'TOTE-CANVAS-WHT', 420000, 520000, 'https://images.unsplash.com/photo-1544816155-12df9643f363?w=1000&auto=format&fit=crop&q=80', 40, 38, 10, 400),
(702, 7, 'TOTE-CANVAS-BLK', 420000, 520000, 'https://images.unsplash.com/photo-1544816155-12df9643f363?w=1000&auto=format&fit=crop&q=80', 40, 38, 10, 400),

-- SP 8: Bình Giữ Nhiệt
(801, 8, 'BOTTLE-MATTE-BLK', 320000, 390000, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=1000&auto=format&fit=crop&q=80', 25, 8, 8, 350),
(802, 8, 'BOTTLE-MATTE-WHT', 350000, 420000, 'https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=1000&auto=format&fit=crop&q=80', 25, 8, 8, 350)
ON CONFLICT (sku) DO NOTHING;

-- 8. Bản đồ Tổ hợp Biến thể (Variant Combinations: option_id -> value_id)
INSERT INTO variant_combinations (variant_id, option_id, value_id, version) VALUES
-- SP 1: Sơ mi
(101, 1, 1, 0), (101, 2, 12, 0), -- Trắng Ngà / M
(102, 1, 1, 0), (102, 2, 13, 0), -- Trắng Ngà / L
(103, 1, 2, 0), (103, 2, 12, 0), -- Đen Than / M
(104, 1, 2, 0), (104, 2, 13, 0), -- Đen Than / L

-- SP 2: Áo Thun
(201, 1, 1, 0), (201, 2, 12, 0), -- Trắng Ngà / M
(202, 1, 1, 0), (202, 2, 13, 0), -- Trắng Ngà / L
(203, 1, 2, 0), (203, 2, 12, 0), -- Đen Than / M
(204, 1, 3, 0), (204, 2, 12, 0), -- Xanh Rêu / M

-- SP 3: Quần Tây
(301, 1, 2, 0), (301, 2, 12, 0), -- Đen Than / M
(302, 1, 2, 0), (302, 2, 13, 0), -- Đen Than / L
(303, 1, 4, 0), (303, 2, 12, 0), -- Be Sáng / M

-- SP 4: Đầm Suông
(401, 1, 1, 0), (401, 2, 11, 0), -- Trắng Ngà / S
(402, 1, 1, 0), (402, 2, 12, 0), -- Trắng Ngà / M
(403, 1, 4, 0), (403, 2, 12, 0), -- Be Sáng / M

-- SP 5: Áo Len
(501, 1, 4, 0), (501, 2, 12, 0), -- Be Sáng / M
(502, 1, 2, 0), (502, 2, 12, 0), -- Đen Than / M

-- SP 6: Chân Váy
(601, 1, 2, 0), (601, 2, 12, 0), -- Đen Than / M

-- SP 7: Túi Tote (chỉ có màu sắc)
(701, 1, 1, 0), -- Trắng Ngà
(702, 1, 2, 0), -- Đen Than

-- SP 8: Bình Giữ Nhiệt
(801, 1, 2, 0), -- Đen Than
(802, 1, 1, 0)  -- Trắng Ngà
ON CONFLICT DO NOTHING;

-- 9. Kho Hàng & Tồn Kho (Warehouses & Inventory Items cho Checkout)
INSERT INTO warehouses (id, code, name, province, district, ward, address_detail, status, version, created_at, updated_at)
VALUES (1, 'WH-HCM-01', 'Kho Tổng TP. Hồ Chí Minh', 'Thành phố Hồ Chí Minh', 'Quận 1', 'Phường Bến Nghé', '123 Lê Lợi', 'ACTIVE', 0, now(), now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO inventory_items (id, warehouse_id, variant_id, on_hand, reserved, version, created_at, updated_at) VALUES
(1, 1, 101, 100, 0, 0, now(), now()),
(2, 1, 102, 100, 0, 0, now(), now()),
(3, 1, 103, 100, 0, 0, now(), now()),
(4, 1, 104, 100, 0, 0, now(), now()),
(5, 1, 201, 100, 0, 0, now(), now()),
(6, 1, 202, 100, 0, 0, now(), now()),
(7, 1, 203, 100, 0, 0, now(), now()),
(8, 1, 204, 100, 0, 0, now(), now()),
(9, 1, 301, 100, 0, 0, now(), now()),
(10, 1, 302, 100, 0, 0, now(), now()),
(11, 1, 303, 100, 0, 0, now(), now()),
(12, 1, 401, 100, 0, 0, now(), now()),
(13, 1, 402, 100, 0, 0, now(), now()),
(14, 1, 403, 100, 0, 0, now(), now()),
(15, 1, 501, 100, 0, 0, now(), now()),
(16, 1, 502, 100, 0, 0, now(), now()),
(17, 1, 601, 100, 0, 0, now(), now()),
(18, 1, 701, 100, 0, 0, now(), now()),
(19, 1, 702, 100, 0, 0, now(), now()),
(20, 1, 801, 100, 0, 0, now(), now()),
(21, 1, 802, 100, 0, 0, now(), now())
ON CONFLICT (warehouse_id, variant_id) DO NOTHING;

-- Cập nhật sequence để các bản ghi mới tạo trong tương lai không bị trùng ID
SELECT setval('users_seq', 100);
SELECT setval('categories_seq', 100);
SELECT setval('options_seq', 100);
SELECT setval('option_values_seq', 100);
SELECT setval('products_seq', 100);
SELECT setval('variants_seq', 1000);
SELECT setval('warehouses_seq', 100);
SELECT setval('inventory_items_seq', 1000);
