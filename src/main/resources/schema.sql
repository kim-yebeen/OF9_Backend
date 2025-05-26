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
    result VARCHAR(10),  -- WIN / LOSE / DRAW /ETC
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
    );

CREATE TABLE IF NOT EXISTS record_companions (
    record_id     INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    companion_id  INT NOT NULL REFERENCES users(id)          ON DELETE CASCADE,
    PRIMARY KEY(record_id, companion_id)
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


-- 1. 감정_lookup 테이블 추가
CREATE TABLE IF NOT EXISTS emotion (
    code SMALLINT PRIMARY KEY,       -- 1~9 숫자 코드
    label VARCHAR(20) NOT NULL       -- "짜릿해요" 등 한글 레이블
);

-- 초기 감정 데이터 삽입 (한 번만 실행)
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

CREATE TABLE IF NOT EXISTS follow_request (
    id SERIAL PRIMARY KEY,
    requester_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_id    INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- 1. 리액션 타입 테이블 (대분류, 소분류)
CREATE TABLE IF NOT EXISTS reaction_type (
                                             id SERIAL PRIMARY KEY,
                                             category VARCHAR(20) NOT NULL,  -- '기쁨축하', '공감응원', '슬픔아쉬움'
    name VARCHAR(20) NOT NULL,      -- '좋아요', '기뻐요', '공감해요' 등
    display_order INT NOT NULL DEFAULT 0
    );

-- 초기 리액션 타입 데이터 삽입
INSERT INTO reaction_type (category, name, display_order) VALUES
('기쁨축하', '좋아요', 1),
('기쁨축하', '기뻐요', 2),
('기쁨축하', '신나요', 3),
('기쁨축하', '멋져요', 4),
('기쁨축하', '짜릿해요',5),
('기쁨축하', '대단해요',6),
('기쁨축하', '축하해요', 7),

('공감응원', '따뜻해요', 8),
('공감응원', '공감해요', 9),
('공감응원', '괜찮아요',10),
('공감응원', '응원해요',11),
('공감응원', '힘내요', 12),

('슬픔아쉬움', '아쉬워요', 13),
('슬픔아쉬움', '속상해요', 14),
('슬픔아쉬움', '슬퍼요', 15)
ON CONFLICT DO NOTHING;

DROP VIEW IF EXISTS record_reaction_stats CASCADE;

-- 2. 사용자 리액션 테이블
CREATE TABLE IF NOT EXISTS record_reaction (
    id SERIAL PRIMARY KEY,
    record_id INT NOT NULL REFERENCES record(record_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reaction_type_id INT NOT NULL REFERENCES reaction_type(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    -- 한 사용자는 하나의 게시물에 하나의 리액션만 가능
    UNIQUE(record_id, user_id)
);

-- 3. 인덱스 추가 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_record_reaction_record_id ON record_reaction(record_id);
CREATE INDEX IF NOT EXISTS idx_record_reaction_user_id ON record_reaction(user_id);
