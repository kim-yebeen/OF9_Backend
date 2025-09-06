-- 기존 테이블들 (이미 CASCADE 설정 완료)
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

-- 감정 테이블
CREATE TABLE IF NOT EXISTS emotion (
                                       code SMALLINT PRIMARY KEY,
                                       label VARCHAR(20) NOT NULL
    );

INSERT INTO emotion (code, label) VALUES
                                      (1, '짜릿해요'),
                                      (2, '만족해요'),
                                      (3, '감동이에요'),
                                      (4, '놀랐어요'),
                                      (5, '행복해요'),
                                      (6, '답답해요'),
                                      (7, '아쉬워요'),
                                      (8, '화났어요'),
                                      (9, '지쳤어요')
    ON CONFLICT DO NOTHING;

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

-- ✅ JPA @ElementCollection이 생성하는 테이블들을 명시적으로 정의
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

-- 리액션 타입 테이블
CREATE TABLE IF NOT EXISTS reaction_type (
                                             display_order INT PRIMARY KEY,
                                             category VARCHAR(20) NOT NULL,
    name VARCHAR(20) NOT NULL,
    UNIQUE(category, name)
    );

INSERT INTO reaction_type (display_order, category, name) VALUES
                                                              (1, '기쁨축하', '좋아요'),
                                                              (2, '기쁨축하', '기뻐요'),
                                                              (3, '기쁨축하', '신나요'),
                                                              (4, '기쁨축하', '멋져요'),
                                                              (5, '기쁨축하', '짜릿해요'),
                                                              (6, '기쁨축하', '대단해요'),
                                                              (7, '기쁨축하', '축하해요'),
                                                              (8, '공감응원', '따뜻해요'),
                                                              (9, '공감응원', '공감해요'),
                                                              (10, '공감응원', '괜찮아요'),
                                                              (11, '공감응원', '응원해요'),
                                                              (12, '공감응원', '힘내요'),
                                                              (13, '슬픔아쉬움', '아쉬워요'),
                                                              (14, '슬픔아쉬움', '속상해요'),
                                                              (15, '슬픔아쉬움', '슬퍼요')
    ON CONFLICT DO NOTHING;

-- 사용자 리액션 테이블
CREATE TABLE IF NOT EXISTS record_reaction (
                                               id SERIAL PRIMARY KEY,
                                               record_id INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction_type_id INT NOT NULL REFERENCES reaction_type(display_order) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(record_id, user_id)
    );

-- 기타 테이블들
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
    type VARCHAR(20) NOT NULL, -- 'FOLLOW', 'FOLLOW_REQUEST', 'REACTION', 'NEW_RECORD', 'SYSTEM'
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    related_user_id INT REFERENCES users(id) ON DELETE CASCADE, -- 알림을 발생시킨 사용자
    related_record_id INT REFERENCES record(record_id) ON DELETE CASCADE, -- 관련 기록
    reaction_type_id INT REFERENCES reaction_type(display_order), -- 공감 타입 (공감 알림일 때)
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS user_block (
                                          id SERIAL PRIMARY KEY,
                                          blocker_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- 차단하는 사용자
    blocked_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- 차단당하는 사용자
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(blocker_id, blocked_id),
    CONSTRAINT no_self_block CHECK (blocker_id <> blocked_id)
    );

-- 성능 최적화를 위한 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_user_block_blocker_id ON user_block(blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_block_blocked_id ON user_block(blocked_id);


-- 성능 최적화 인덱스
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications(type);
CREATE INDEX IF NOT EXISTS idx_notifications_is_read ON notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_record_reaction_record_id ON record_reaction(record_id);
CREATE INDEX IF NOT EXISTS idx_record_reaction_user_id ON record_reaction(user_id);
CREATE INDEX IF NOT EXISTS idx_record_user_id ON record(user_id);
CREATE INDEX IF NOT EXISTS idx_record_companions_record_id ON record_companions(record_id);
CREATE INDEX IF NOT EXISTS idx_record_companions_companion_id ON record_companions(companion_id);
