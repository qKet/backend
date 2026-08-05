USE qket;

-- ins_id/ins_ip/ins_de: 등록자ID/등록IP/등록일시, upt_id/upt_ip/upt_de: 수정자ID/수정IP/수정일시
-- ins_id 기본값 'SYSTEM': 로그인 전(회원가입 등) 처럼 행위자가 없는 INSERT에 사용
-- upt_* 는 NULL 허용: 아직 한 번도 수정 안 된 행은 비어있는 게 정상
-- FK를 안 건 이유: 'SYSTEM' 같은 고정값이 USERS.user_id 에 실재하지 않고,
--   회원 탈퇴/삭제가 감사 기록까지 막지 않게 하기 위함

CREATE TABLE IF NOT EXISTS ROLES (
    role_id BIGINT NOT NULL AUTO_INCREMENT,
    role_name VARCHAR(255),

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_name (role_name)
);

CREATE TABLE IF NOT EXISTS VENUE (
    venue_id BIGINT NOT NULL AUTO_INCREMENT,
    venue_name VARCHAR(255) NOT NULL,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (venue_id),
    UNIQUE KEY uk_venue_name (venue_name)
);

-- CATEGORIES: 공연 카테고리(콘서트/뮤지컬 등). 사용자가 홈 화면에서 카테고리를 선택해 공연을 필터링하는 데 사용
CREATE TABLE IF NOT EXISTS CATEGORIES (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_nm VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (category_id),
    UNIQUE KEY uk_categories_category_nm (category_nm)
    );


CREATE TABLE IF NOT EXISTS PERFORMANCES (
    performance_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    p_title VARCHAR(255) NOT NULL,
    venue_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    poster_url VARCHAR(500),
    created_per DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (venue_id) REFERENCES VENUE (venue_id),
    FOREIGN KEY (category_id) REFERENCES CATEGORIES (category_id)
);

-- user_id: 회원가입 시 입력받는 로그인 아이디를 그대로 PK로 사용 (auto_increment 아님)
-- 소셜 로그인 계정은 user_id를 "{provider}_{providerUserId}" 형태로 자동 생성하고 pwd는 NULL로 둠
-- login_provider/provider_user_id: 소셜 로그인 연동 정보. LOCAL(일반 가입) 계정은 provider_user_id가 NULL
CREATE TABLE IF NOT EXISTS USERS (
    user_id VARCHAR(50) NOT NULL,
    user_nm VARCHAR(255) NOT NULL,
    pwd VARCHAR(255) NULL,
    user_email VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    user_status VARCHAR(255) NOT NULL,
    created_user DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    login_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_user_id VARCHAR(255) NULL,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id),
    FOREIGN KEY (role_id) REFERENCES ROLES (role_id),
    UNIQUE KEY uk_users_user_email (user_email),
    UNIQUE KEY uk_users_provider (login_provider, provider_user_id)
);

-- open_time: 예매 오픈 시각. round_time(공연 시작 시각)과 별개로 "언제부터 예매 가능한지"를 나타냄
-- 예매 로직: NOW() < open_time 이면 예매 시도를 거부 (오픈런 시점 제어)
CREATE TABLE IF NOT EXISTS PERFORMANCE_ROUND (
    round_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    performance_id BIGINT NOT NULL,
    round_time DATETIME NOT NULL COMMENT '공연(회차) 시작 시각',
    open_time DATETIME NOT NULL COMMENT '예매 오픈 시각 (이 시각 이전에는 예매 불가)',
    round_status VARCHAR(255) NOT NULL,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (performance_id) REFERENCES PERFORMANCES (performance_id)
);

-- PERFORMANCE_CAST: 공연 캐스팅(출연 배우/배역) — PER02_DETAIL01(공연 상세 조회)
-- 캐스팅표 엑셀 업로드는 이 테이블에 여러 행을 bulk insert 하는 API로 처리 (스키마는 동일)
-- round_id: NULL 이면 전체 회차 공통 캐스팅, 값이 있으면 그 회차 전용(더블/트리플 캐스팅 대응)
-- [주의] round_id 는 nullable FK — MyBatis update 문에서 <if test="roundId != null"> 가드를 쓰면 안 됨
--   ("전체 회차 공통으로 되돌리기"가 정상 케이스라 부분요청과 구분이 안 됨. CLAUDE.md 참고)
CREATE TABLE IF NOT EXISTS PERFORMANCE_CAST (
    cast_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    performance_id BIGINT NOT NULL,
    round_id BIGINT NULL COMMENT 'NULL이면 전체 회차 공통, 값이 있으면 해당 회차 전용',
    actor_nm VARCHAR(100) NOT NULL COMMENT '배우 이름',
    casting_nm VARCHAR(100) NULL COMMENT '배역명 (ERD 기준 컬럼명)',
    sort_order INT NOT NULL DEFAULT 0,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (performance_id) REFERENCES PERFORMANCES (performance_id),
    FOREIGN KEY (round_id) REFERENCES PERFORMANCE_ROUND (round_id),
    KEY idx_cast_performance (performance_id, sort_order)
);

-- SEATS: round_id, status 제거. 공연장(venue) 소속으로 변경 (회차 구분 없이 좌석 자체는 공연장에 귀속)
CREATE TABLE IF NOT EXISTS SEATS (
    seat_id BIGINT NOT NULL AUTO_INCREMENT,
    venue_id BIGINT NOT NULL,
    seat_row VARCHAR(255) NOT NULL,
    seat_colume VARCHAR(255) NOT NULL,
    grade VARCHAR(255) NOT NULL,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (seat_id),
    FOREIGN KEY (venue_id) REFERENCES VENUE (venue_id),
    UNIQUE KEY uk_seats_venue_seat (venue_id, seat_row, seat_colume)
);

-- RESERVATIONS: performance_id 추가 (좌석-회차 슬롯을 회차 등록 시점에 미리 생성, user_id는 예매 전까지 NULL)
-- 예매하기 버튼 클릭 시: 애플리케이션 단에서 먼저 PERFORMANCE_ROUND.open_time <= NOW() 인지 확인한 뒤
--   UPDATE RESERVATIONS SET user_id=?, reserved_status='RESERVED', reserved_at=NOW()
--   WHERE seat_id=? AND round_id=? AND user_id IS NULL  (조건부 UPDATE로 중복예매 방지)
CREATE TABLE IF NOT EXISTS RESERVATIONS (
    reservation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NULL,
    seat_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    performance_id BIGINT NOT NULL,
    reserved_status VARCHAR(255) NOT NULL DEFAULT 'AVAILABLE',
    created_reserved DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '슬롯이 생성된 시각 (회차 등록 시점)',
    reserved_at DATETIME NULL COMMENT '실제 예매(버튼 클릭) 시각, 예매 전까지 NULL',

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL COMMENT '가장 최근에 예매/취소한 사용자',
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES USERS (user_id),
    FOREIGN KEY (seat_id) REFERENCES SEATS (seat_id),
    FOREIGN KEY (round_id) REFERENCES PERFORMANCE_ROUND (round_id),
    FOREIGN KEY (performance_id) REFERENCES PERFORMANCES (performance_id),
    UNIQUE KEY uk_reservations_seat_round (seat_id, round_id)
);

CREATE TABLE IF NOT EXISTS RESERVATION_HISTORY (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL,
    seat_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    performance_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,  -- 'RESERVED' / 'CANCELLED'
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '이 이력을 발생시킨 사용자 (user_id와 항상 동일)',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES USERS(user_id),
    FOREIGN KEY (seat_id) REFERENCES SEATS(seat_id),
    FOREIGN KEY (round_id) REFERENCES PERFORMANCE_ROUND(round_id),
    FOREIGN KEY (performance_id) REFERENCES PERFORMANCES(performance_id)
);

-- PROGRAMS: 프로그램관리 — 프론트 화면(라우트) 등록. program_type: 'MENU'(네비게이션에 노출) / 'PAGE'(URL 접근만, 메뉴 미노출)
CREATE TABLE IF NOT EXISTS PROGRAMS (
    program_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    program_nm VARCHAR(255) NOT NULL,
    url_path VARCHAR(255) NOT NULL,
    program_type VARCHAR(20) NOT NULL,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_programs_url_path (url_path)
);

-- ROLE_PROGRAMS: 권한 확장 — 역할별로 어떤 프로그램(화면)에 접근 가능한지 매핑
CREATE TABLE IF NOT EXISTS ROLE_PROGRAMS (
    role_id BIGINT NOT NULL,
    program_id BIGINT NOT NULL,

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id, program_id),
    FOREIGN KEY (role_id) REFERENCES ROLES (role_id),
    FOREIGN KEY (program_id) REFERENCES PROGRAMS (program_id)
);

-- MENUS: 메뉴관리 — 네비게이션 트리 구조. parent_menu_id로 자기참조 (그리드 화면에서 순서/계층 관리)
-- program_id는 NULL 허용 — 연결된 페이지 없이 하위메뉴만 묶는 "그룹 전용" 메뉴(예: 상단 "관리자" 드롭다운)를 만들기 위함
CREATE TABLE IF NOT EXISTS MENUS (
    menu_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    program_id BIGINT NULL,
    parent_menu_id BIGINT NULL,
    menu_nm VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    use_yn CHAR(1) NOT NULL DEFAULT 'Y',

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (program_id) REFERENCES PROGRAMS (program_id),
    FOREIGN KEY (parent_menu_id) REFERENCES MENUS (menu_id)
);

-- PAYMENTS: 토스페이먼츠 결제 승인 내역. reservation_id 는 결제 승인 성공 후 확정된 예매 슬롯을 가리킴
-- (결제 실패/취소 시엔 행 자체가 안 생김 — 승인 성공 건만 기록)
CREATE TABLE IF NOT EXISTS PAYMENTS (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    reservation_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    payment_key VARCHAR(200) NOT NULL,
    amount BIGINT NOT NULL,
    pay_status VARCHAR(50) NOT NULL,
    approved_at DATETIME NULL,
    deleted_yn CHAR(1) NOT NULL DEFAULT 'N' COMMENT '결제내역 목록에서 사용자가 지운 건 Y — 회계 기록 보존을 위해 실제 행은 안 지우고 숨기기만 함',

    ins_id VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ins_ip VARCHAR(45) NULL,
    ins_de DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    upt_id VARCHAR(50) NULL,
    upt_ip VARCHAR(45) NULL,
    upt_de DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (reservation_id) REFERENCES RESERVATIONS (reservation_id),
    FOREIGN KEY (user_id) REFERENCES USERS (user_id),
    UNIQUE KEY uk_payments_order_id (order_id),
    UNIQUE KEY uk_payments_payment_key (payment_key)
);

SHOW TABLES;
