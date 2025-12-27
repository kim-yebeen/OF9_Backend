-- 기존 테이블들
CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     kakao_id BIGINT UNIQUE NOT NULL,
                                     nickname VARCHAR(50) NOT NULL UNIQUE,
    profile_image_url TEXT,
    fav_team VARCHAR(50),
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
    );


CREATE TABLE IF NOT EXISTS user_follow (
                                           follower_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    followee_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT no_self_follow CHECK (follower_id <> followee_id)
    );

CREATE TABLE IF NOT EXISTS game (
                                    game_id VARCHAR PRIMARY KEY,
                                    date DATE NOT NULL,
                                    time TIME,
                                    stadium VARCHAR(50),
    home_team VARCHAR(50),
    away_team VARCHAR(50),
    home_score INT,
    away_score INT,
    status VARCHAR(20),
    home_img TEXT,
    away_img TEXT
    );

-- ========================================
-- 감정 테이블 (수정됨)
-- ========================================

-- 감정 마스터 테이블 (고유한 감정 태그)
CREATE TABLE IF NOT EXISTS emotion (
                                       code SMALLINT PRIMARY KEY,
                                       label VARCHAR(20) NOT NULL UNIQUE
    );

-- 감정 데이터 삽입 (중복 제거된 16개)
INSERT INTO emotion (code, label) VALUES
                                      (1, '행복해요'),
                                      (2, '놀랐어요'),
                                      (3, '짜릿해요'),
                                      (4, '벅차요'),
                                      (5, '통쾌해요'),
                                      (6, '만족해요'),
                                      (7, '지루해요'),
                                      (8, '무난해요'),
                                      (9, '긴장돼요'),
                                      (10, '질투나요'),
                                      (11, '답답해요'),
                                      (12, '아쉬워요'),
                                      (13, '지쳤어요'),
                                      (14, '허탈해요'),
                                      (15, '짜증나요'),
                                      (16, '화나요')
    ON CONFLICT (code) DO NOTHING;

-- 카테고리별 감정 매핑 테이블
CREATE TABLE IF NOT EXISTS emotion_category (
                                                id SERIAL PRIMARY KEY,
                                                emotion_code SMALLINT NOT NULL REFERENCES emotion(code) ON DELETE CASCADE,
    category VARCHAR(20) NOT NULL,  -- '전체', '승리', '무승부', '패배'
    display_order INT NOT NULL,     -- UI 표시 순서
    UNIQUE(emotion_code, category)
    );

-- 카테고리별 감정 매핑 데이터
-- 전체 (16개 - 모든 감정)
INSERT INTO emotion_category (emotion_code, category, display_order) VALUES
                                                                         (1, '전체', 1),
                                                                         (2, '전체', 2),
                                                                         (3, '전체', 3),
                                                                         (4, '전체', 4),
                                                                         (5, '전체', 5),
                                                                         (6, '전체', 6),
                                                                         (7, '전체', 7),
                                                                         (8, '전체', 8),
                                                                         (9, '전체', 9),
                                                                         (10, '전체', 10),
                                                                         (11, '전체', 11),
                                                                         (12, '전체', 12),
                                                                         (13, '전체', 13),
                                                                         (14, '전체', 14),
                                                                         (15, '전체', 15),
                                                                         (16, '전체', 16)
    ON CONFLICT DO NOTHING;

-- 승리 (9개)
INSERT INTO emotion_category (emotion_code, category, display_order) VALUES
                                                                         (1, '승리', 1),  -- 행복해요
                                                                         (2, '승리', 2),  -- 놀랐어요
                                                                         (3, '승리', 3),  -- 짜릿해요
                                                                         (4, '승리', 4),  -- 벅차요
                                                                         (5, '승리', 5),  -- 통쾌해요
                                                                         (6, '승리', 6),  -- 만족해요
                                                                         (7, '승리', 7),  -- 지루해요
                                                                         (8, '승리', 8),  -- 무난해요
                                                                         (9, '승리', 9)   -- 긴장돼요
    ON CONFLICT DO NOTHING;

-- 무승부 (11개)
INSERT INTO emotion_category (emotion_code, category, display_order) VALUES
                                                                         (3, '무승부', 1),   -- 짜릿해요
                                                                         (7, '무승부', 2),   -- 지루해요
                                                                         (8, '무승부', 3),   -- 무난해요
                                                                         (10, '무승부', 4),  -- 질투나요
                                                                         (11, '무승부', 5),  -- 답답해요
                                                                         (9, '무승부', 6),   -- 긴장돼요
                                                                         (12, '무승부', 7),  -- 아쉬워요
                                                                         (13, '무승부', 8),  -- 지쳤어요
                                                                         (14, '무승부', 9),  -- 허탈해요
                                                                         (15, '무승부', 10), -- 짜증나요
                                                                         (16, '무승부', 11)  -- 화나요
    ON CONFLICT DO NOTHING;

-- 패배 (10개)
INSERT INTO emotion_category (emotion_code, category, display_order) VALUES
                                                                         (7, '패배', 1),   -- 지루해요
                                                                         (8, '패배', 2),   -- 무난해요
                                                                         (10, '패배', 3),  -- 질투나요
                                                                         (11, '패배', 4),  -- 답답해요
                                                                         (9, '패배', 5),   -- 긴장돼요
                                                                         (12, '패배', 6),  -- 아쉬워요
                                                                         (13, '패배', 7),  -- 지쳤어요
                                                                         (14, '패배', 8),  -- 허탈해요
                                                                         (15, '패배', 9),  -- 짜증나요
                                                                         (16, '패배', 10)  -- 화나요
    ON CONFLICT DO NOTHING;

-- ========================================
-- 나머지 테이블들
-- ========================================

CREATE TABLE IF NOT EXISTS record (
                                      record_id SERIAL PRIMARY KEY,
                                      user_id INT REFERENCES users(id) ON DELETE CASCADE,
    game_id VARCHAR REFERENCES game(game_id) ON DELETE NO ACTION,
    seat_info VARCHAR(100),
    stadium VARCHAR(50) NOT NULL,
    comment TEXT,
    long_content TEXT,
    emotion_code SMALLINT NOT NULL REFERENCES emotion(code),
    best_player VARCHAR(50),
    food_tags TEXT[],
    media_urls TEXT[],
    result VARCHAR(10),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS record_companions (
                                                 record_id     INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    companion_id  INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY(record_id, companion_id)
    );

CREATE TABLE IF NOT EXISTS record_food_tags (
                                                record_id INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    tag VARCHAR(255) NOT NULL,
    PRIMARY KEY(record_id, tag)
    );

CREATE TABLE IF NOT EXISTS record_media_urls (
                                                 record_id INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    PRIMARY KEY(record_id, url)
    );

CREATE TABLE IF NOT EXISTS follow_request (
                                              id SERIAL PRIMARY KEY,
                                              requester_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id    INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS record_like (
                                           id SERIAL PRIMARY KEY,
                                           record_id INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(record_id, user_id)
    );

CREATE TABLE IF NOT EXISTS record_comment (
                                              id SERIAL PRIMARY KEY,
                                              record_id INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_comment_id INT REFERENCES record_comment(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS stadium (
                                       id SERIAL PRIMARY KEY,
                                       name VARCHAR(50) NOT NULL
    );

CREATE TABLE IF NOT EXISTS food (
                                    id SERIAL PRIMARY KEY,
                                    fnb_name VARCHAR(50) NOT NULL,
    stadium_id INT REFERENCES stadium(id)
    );

CREATE TABLE IF NOT EXISTS notifications (
                                             id SERIAL PRIMARY KEY,
                                             user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    related_user_id INT REFERENCES users(id) ON DELETE CASCADE,
    related_record_id INT REFERENCES record(record_id) ON DELETE CASCADE,
    related_comment_id INT REFERENCES record_comment(id) ON DELETE CASCADE,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS user_block (
                                          id SERIAL PRIMARY KEY,
                                          blocker_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(blocker_id, blocked_id),
    CONSTRAINT no_self_block CHECK (blocker_id <> blocked_id)
    );




CREATE TABLE IF NOT EXISTS player (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(50) NOT NULL,
    team VARCHAR(10) NOT NULL,
    position VARCHAR(20) NOT NULL
    );

-- 신고 테이블
-- 신고 테이블
CREATE TABLE IF NOT EXISTS complaints (
                                          id SERIAL PRIMARY KEY,
                                          reporter_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id INT REFERENCES users(id) ON DELETE SET NULL,
    reported_record_id INT REFERENCES record(record_id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    report_date DATE NOT NULL DEFAULT CURRENT_DATE,  -- ✅ 날짜 컬럼 추가

-- 같은 날 중복 신고 방지
    CONSTRAINT unique_complaint_per_day UNIQUE(reporter_id, reported_user_id, reported_record_id, report_date)
    );

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_complaints_reporter_id ON complaints(reporter_id);
CREATE INDEX IF NOT EXISTS idx_complaints_reported_user_id ON complaints(reported_user_id);
CREATE INDEX IF NOT EXISTS idx_complaints_reported_record_id ON complaints(reported_record_id);
CREATE INDEX IF NOT EXISTS idx_complaints_created_at ON complaints(created_at);
CREATE INDEX IF NOT EXISTS idx_complaints_report_date ON complaints(report_date);


-- 인덱스
CREATE INDEX IF NOT EXISTS idx_player_name ON player(name);
CREATE INDEX IF NOT EXISTS idx_user_block_blocker_id ON user_block(blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_block_blocked_id ON user_block(blocked_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_record_like_record_id ON record_like(record_id);
CREATE INDEX IF NOT EXISTS idx_record_like_user_id ON record_like(user_id);
CREATE INDEX IF NOT EXISTS idx_record_comment_record_id ON record_comment(record_id);
CREATE INDEX IF NOT EXISTS idx_record_comment_user_id ON record_comment(user_id);
CREATE INDEX IF NOT EXISTS idx_record_comment_parent_id ON record_comment(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_record_user_id ON record(user_id);
CREATE INDEX IF NOT EXISTS idx_record_companions_record_id ON record_companions(record_id);
CREATE INDEX IF NOT EXISTS idx_record_companions_companion_id ON record_companions(companion_id);
CREATE INDEX IF NOT EXISTS idx_emotion_category_category ON emotion_category(category);
CREATE INDEX IF NOT EXISTS idx_emotion_category_emotion_code ON emotion_category(emotion_code);