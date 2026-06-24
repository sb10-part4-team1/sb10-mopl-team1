-- ==========================================
-- 독립 테이블 (외래키 의존성이 없는 테이블)
-- ==========================================

-- 유저 테이블
-- [소프트 삭제 & 익명화 정책]
-- 1. 회원 탈퇴 시 즉시 물리 삭제하지 않고 `is_deleted = true`, `deleted_at = NOW()`로 설정하여 30일간 복구 유예 기간을 가집니다.
-- 2. 30일이 지난 후에는 개인정보 보호법 준수를 위해 PII(개인식별정보) 데이터만 비식별 랜덤값으로 업데이트(익명화)합니다.
-- 3. 유저 Row 자체는 DB에 유지되므로, DM 메시지 등 외래키가 걸린 다른 테이블의 참조 무결성을 깨뜨리지 않습니다.
CREATE TABLE "users" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "name"               VARCHAR(50)                 NOT NULL, -- 익명화 시 "(탈퇴한 사용자)"로 변경
    "email"              VARCHAR(255)                NOT NULL UNIQUE, -- 중복 가입 허용을 위해 익명화 시 "deleted_uuid@mopl.com" 형태로 고유값 변경
    "password"           VARCHAR(255)                NOT NULL, -- 익명화 시 무작위 해시값으로 변경하여 로그인 차단
    "profile_image_url"  TEXT                        NULL, -- 익명화 시 NULL 처리
    "role"               VARCHAR(20)                 NOT NULL,
    "is_locked"          BOOLEAN      DEFAULT false  NOT NULL,
    "is_deleted"         BOOLEAN      DEFAULT false  NOT NULL,
    "deleted_at"         TIMESTAMP WITH TIME ZONE    NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "updated_at"         TIMESTAMP WITH TIME ZONE    NOT NULL
);

-- 컨텐츠 테이블
CREATE TABLE "contents" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "title"              VARCHAR(100)                NOT NULL,
    "type"               VARCHAR(20)                 NOT NULL,
    "description"        TEXT                        NOT NULL,
    "thumbnail_url"      TEXT                        NULL,
    "average_rating"     NUMERIC(2,1) DEFAULT 0.0    NOT NULL,
    "review_count"       INT                         NOT NULL,
    "watcher_count"      BIGINT                      NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "updated_at"         TIMESTAMP WITH TIME ZONE    NOT NULL
);

-- 태그 테이블
CREATE TABLE "tags" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "name"               VARCHAR(100)                NOT NULL UNIQUE,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL
);

-- 대화방 테이블
CREATE TABLE "conversations" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "updated_at"         TIMESTAMP WITH TIME ZONE    NOT NULL
);

-- ==========================================
-- 관계 및 매핑 테이블 (외래키 포함)
-- ==========================================

-- 임시 비밀번호 테이블
CREATE TABLE "temporary_passwords" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "user_id"            UUID                        NOT NULL UNIQUE,
    "password"           VARCHAR(255)                NOT NULL,
    "expires_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "FK_USERS_TO_TEMP_PASSWORDS"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 토큰 테이블
CREATE TABLE "refresh_tokens" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "user_id"            UUID                        NOT NULL,
    "token"              TEXT                        NOT NULL UNIQUE,
    "expires_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "FK_USERS_TO_REFRESH_TOKENS"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 소셜 계정 테이블 (소셜 정보는 복합 유니크이므로 하단 배치)
CREATE TABLE "social_accounts" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "user_id"            UUID                        NOT NULL,
    "provider"           VARCHAR(20)                 NOT NULL,
    "provider_user_id"   VARCHAR(255)                NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "UQ_SOCIAL_ACCOUNTS_PROVIDER"
        UNIQUE ("provider", "provider_user_id"),
    CONSTRAINT "FK_USERS_TO_SOCIAL_ACCOUNTS"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 팔로우 테이블 (복합 유니크 하단 배치)
CREATE TABLE "follows" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "follower_id"        UUID                        NOT NULL,
    "followee_id"        UUID                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "UQ_FOLLOWS_RELATION"
        UNIQUE ("follower_id", "followee_id"),
    CONSTRAINT "CK_FOLLOWS_SELF"
        CHECK ("follower_id" <> "followee_id"),
    CONSTRAINT "FK_USERS_TO_FOLLOWS_FOLLOWER"
        FOREIGN KEY ("follower_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_USERS_TO_FOLLOWS_FOLLOWEE"
        FOREIGN KEY ("followee_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 컨텐츠-태그 매핑 테이블 (복합 유니크 하단 배치)
CREATE TABLE "content_tags" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "content_id"         UUID                        NOT NULL,
    "tag_id"             UUID                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "UQ_CONTENT_TAGS"
        UNIQUE ("content_id", "tag_id"),
    CONSTRAINT "FK_CONTENTS_TO_CONTENT_TAGS"
        FOREIGN KEY ("content_id") REFERENCES "contents" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_TAGS_TO_CONTENT_TAGS"
        FOREIGN KEY ("tag_id") REFERENCES "tags" ("id") ON DELETE CASCADE
);

-- 재생목록 테이블
CREATE TABLE "playlists" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "owner_id"           UUID                        NOT NULL,
    "title"              VARCHAR(255)                NOT NULL,
    "description"        TEXT                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "updated_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "FK_USERS_TO_PLAYLISTS"
        FOREIGN KEY ("owner_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 재생목록-컨텐츠 매핑 테이블 (복합 유니크 하단 배치)
CREATE TABLE "playlist_contents" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "playlist_id"        UUID                        NOT NULL,
    "content_id"         UUID                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "UQ_PLAYLIST_CONTENTS"
        UNIQUE ("playlist_id", "content_id"),
    CONSTRAINT "FK_PLAYLISTS_TO_PLAYLIST_CONTENTS"
        FOREIGN KEY ("playlist_id") REFERENCES "playlists" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_CONTENTS_TO_PLAYLIST_CONTENTS"
        FOREIGN KEY ("content_id") REFERENCES "contents" ("id") ON DELETE CASCADE
);

-- 재생목록 구독 테이블 (복합 유니크 하단 배치)
CREATE TABLE "playlist_subscriptions" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "playlist_id"        UUID                        NOT NULL,
    "subscriber_id"      UUID                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "UQ_PLAYLIST_SUBSCRIPTIONS"
        UNIQUE ("playlist_id", "subscriber_id"),
    CONSTRAINT "FK_PLAYLISTS_TO_PLAYLIST_SUBSCRIPTIONS"
        FOREIGN KEY ("playlist_id") REFERENCES "playlists" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_USERS_TO_PLAYLIST_SUBSCRIPTIONS"
        FOREIGN KEY ("subscriber_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 대화방 참여자 테이블 (복합 PK이므로 컬럼 라인에서 분리 후 정렬)
CREATE TABLE "conversation_participants" (
    "conversation_id"    UUID                        NOT NULL,
    "user_id"            UUID                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "PK_CONVERSATION_PARTICIPANTS"
        PRIMARY KEY ("conversation_id", "user_id"),
    CONSTRAINT "FK_CONVERSATIONS_TO_CONV_PARTICIPANTS"
        FOREIGN KEY ("conversation_id") REFERENCES "conversations" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_USERS_TO_CONV_PARTICIPANTS"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- DM 메시지 테이블
-- [참조 무결성 보존]
-- 유저 탈퇴 시 물리 삭제가 아닌 익명화(Anonymization)를 진행하므로,
-- 발송자(sender_id)와 수신자(receiver_id)는 NOT NULL로 유지되며 ON DELETE CASCADE가 걸리지 않습니다.
CREATE TABLE "direct_messages" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "conversation_id"    UUID                        NOT NULL,
    "sender_id"          UUID                        NOT NULL,
    "receiver_id"        UUID                        NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "content"            TEXT                        NOT NULL,
    "is_read"            BOOLEAN DEFAULT false       NOT NULL,
    CONSTRAINT "FK_CONVERSATIONS_TO_DIRECT_MESSAGES"
        FOREIGN KEY ("conversation_id") REFERENCES "conversations" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_USERS_TO_DM_SENDER"
        FOREIGN KEY ("sender_id") REFERENCES "users" ("id"),
    CONSTRAINT "FK_USERS_TO_DM_RECEIVER"
        FOREIGN KEY ("receiver_id") REFERENCES "users" ("id")
);

-- 알림 테이블
CREATE TABLE "notifications" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "user_id"            UUID                        NOT NULL,
    "title"              VARCHAR(255)                NOT NULL,
    "content"            VARCHAR(255)                NOT NULL,
    "level"              VARCHAR(20)                 NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "is_read"            BOOLEAN DEFAULT false       NOT NULL,
    CONSTRAINT "FK_USERS_TO_NOTIFICATIONS"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);

-- 시청 세션 테이블
CREATE TABLE "watching_session" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "watcher_id"         UUID                        NOT NULL,
    "content_id"         UUID                        NOT NULL,
    CONSTRAINT "FK_USERS_TO_WATCHING_SESSION"
        FOREIGN KEY ("watcher_id") REFERENCES "users" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_CONTENTS_TO_WATCHING_SESSION"
        FOREIGN KEY ("content_id") REFERENCES "contents" ("id") ON DELETE CASCADE
);

-- 컨텐츠 리뷰 테이블 (복합 유니크 하단 배치)
CREATE TABLE "content_reviews" (
    "id"                 UUID                        NOT NULL PRIMARY KEY,
    "content_id"         UUID                        NOT NULL,
    "user_id"            UUID                        NOT NULL,
    "content"            TEXT                        NOT NULL,
    "rating"             NUMERIC(2,1)                NOT NULL,
    "created_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    "updated_at"         TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT "UQ_CONTENT_REVIEWS_USER"
        UNIQUE ("content_id", "user_id"),
    CONSTRAINT "FK_CONTENTS_TO_CONTENT_REVIEWS"
        FOREIGN KEY ("content_id") REFERENCES "contents" ("id") ON DELETE CASCADE,
    CONSTRAINT "FK_USERS_TO_CONTENT_REVIEWS"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE CASCADE
);


-- ==========================================
-- 성능 최적화를 위한 조회용 인덱스 (INDEX)
-- ==========================================
CREATE INDEX "IDX_REFRESH_TOKENS_USER"
    ON "refresh_tokens" ("user_id");

CREATE INDEX "IDX_FOLLOWS_FOLLOWEE"
    ON "follows" ("followee_id");

CREATE INDEX "IDX_CONTENT_TAGS_TAG"
    ON "content_tags" ("tag_id");

CREATE INDEX "IDX_CONV_PARTICIPANTS_USER"
    ON "conversation_participants" ("user_id");

CREATE INDEX "IDX_DM_CONVERSATION_TIME"
    ON "direct_messages" ("conversation_id", "created_at");

CREATE INDEX "IDX_NOTIFICATIONS_USER_READ"
    ON "notifications" ("user_id", "is_read");

CREATE INDEX "IDX_WATCHING_SESSION_WATCHER"
    ON "watching_session" ("watcher_id");

CREATE INDEX "IDX_CONTENTS_CREATED_AT"
    ON "contents" ("created_at");
