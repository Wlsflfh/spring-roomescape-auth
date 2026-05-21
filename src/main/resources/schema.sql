CREATE TABLE reservation_time (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    start_at   TIME      NOT NULL UNIQUE,
    PRIMARY KEY (id)
);

CREATE TABLE store (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE theme (
    id                 BIGINT           NOT NULL AUTO_INCREMENT,
    name               VARCHAR(255)     NOT NULL UNIQUE,
    description        VARCHAR(255)     NOT NULL,
    thumbnail_url      VARCHAR(1024)    NOT NULL,
    store_id           BIGINT           NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (store_id) REFERENCES store (id)
);

CREATE TABLE member (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    login_id    VARCHAR(255)    NOT NULL UNIQUE,
    name        VARCHAR(50)     NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'MEMBER',
    PRIMARY KEY (id),
    CONSTRAINT chk_member_role CHECK (role IN ('MEMBER', 'MANAGER'))
);

CREATE TABLE manager_store (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    manager_id  BIGINT  NOT NULL,
    store_id    BIGINT  NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (manager_id) REFERENCES member (id),
    FOREIGN KEY (store_id)   REFERENCES store (id),
    CONSTRAINT uq_manager_store UNIQUE (manager_id, store_id)
);

CREATE TABLE reservation (
    id                 BIGINT          NOT NULL AUTO_INCREMENT,
    member_id          BIGINT          NOT NULL,
    reservation_date   DATE            NOT NULL,
    time_id            BIGINT          NOT NULL,
    theme_id           BIGINT          NOT NULL,
    status             VARCHAR(20)     NOT NULL DEFAULT 'RESERVED',
    PRIMARY KEY (id),
    FOREIGN KEY (member_id) REFERENCES member (id),
    FOREIGN KEY (time_id)  REFERENCES reservation_time (id),
    FOREIGN KEY (theme_id) REFERENCES theme (id),
    CONSTRAINT chk_reservation_status
        CHECK (status IN ('RESERVED', 'CANCELED'))
);
