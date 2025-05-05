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

CREATE TABLE user_follow (
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
    emotion_emoji VARCHAR(20),
    best_player VARCHAR(50),
    food_tags TEXT[],
    media_urls TEXT[],
    result VARCHAR(10), -- WIN / LOSE / DRAW
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
