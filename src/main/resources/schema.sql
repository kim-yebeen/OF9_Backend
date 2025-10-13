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

DROP TABLE IF EXISTS record_reaction CASCADE;
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
    parent_comment_id INT REFERENCES record_comment(id) ON DELETE CASCADE,  -- 대댓글용
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP  -- soft delete용
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

DROP TABLE IF EXISTS notifications CASCADE;
CREATE TABLE notifications (
                               id SERIAL PRIMARY KEY,
                               user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type VARCHAR(20) NOT NULL, -- 'FOLLOW', 'FOLLOW_REQUEST', 'LIKE', 'NEW_RECORD', 'COMMENT', 'REPLY', 'SYSTEM'
                               title VARCHAR(100) NOT NULL,
                               content TEXT NOT NULL,
                               related_user_id INT REFERENCES users(id) ON DELETE CASCADE, -- 알림을 발생시킨 사용자
                               related_record_id INT REFERENCES record(record_id) ON DELETE CASCADE, -- 관련 기록
                               related_comment_id INT REFERENCES record_comment(id) ON DELETE CASCADE, -- 관련 댓글
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

-- 뱃지 종류를 정의하는 테이블
CREATE TABLE IF NOT EXISTS badge (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,         -- 뱃지 카테고리 (예: STADIUM, WINS)
    name VARCHAR(100) NOT NULL UNIQUE,      -- 뱃지 이름 (예: 잠실 정복, 승리요정 입문)
    description TEXT,                       -- 뱃지 설명
    image_url TEXT,                         -- 뱃지 이미지 URL
    threshold INT                           -- 뱃지 획득 조건 값 (예: 5회, 10승)
    );

-- 사용자가 획득한 뱃지를 기록하는 테이블
CREATE TABLE IF NOT EXISTS user_badge (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id INT NOT NULL REFERENCES badge(id) ON DELETE CASCADE,
    achieved_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, badge_id)
    );

CREATE TABLE IF NOT EXISTS player (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(50) NOT NULL,
    team VARCHAR(10) NOT NULL,
    position VARCHAR(20) NOT NULL
    );

CREATE INDEX idx_player_name ON player(name);
CREATE INDEX IF NOT EXISTS idx_user_badge_user_id ON user_badge(user_id);
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

DROP TABLE IF EXISTS reaction_type CASCADE;