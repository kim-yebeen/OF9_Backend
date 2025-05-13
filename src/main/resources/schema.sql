CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    kakao_id BIGINT UNIQUE NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    profile_image_url TEXT,
    body VARCHAR(100),
    fav_team VARCHAR(50),
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
    playtime TIME,
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
    user_id INT REFERENCES users(id),
    game_id VARCHAR REFERENCES game(game_id),
    seat_info VARCHAR(100),
    comment TEXT,
    emotion_code SMALLINT NOT NULL REFERENCES emotion(code),
    best_player VARCHAR(50),
    food_tags TEXT[],
    media_urls TEXT[],
    result VARCHAR(10),  -- WIN / LOSE / DRAW
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
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