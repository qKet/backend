USE qket;
SET NAMES utf8mb4;

-- [주의] 기존 데이터 초기화 - 재실행 시에만 주석 해제하여 사용
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE RESERVATIONS;
-- TRUNCATE TABLE SEATS;
-- TRUNCATE TABLE PERFORMANCE_ROUND;
-- TRUNCATE TABLE PERFORMANCES;
-- TRUNCATE TABLE VENUE;
-- TRUNCATE TABLE USERS;
-- TRUNCATE TABLE ROLES;
-- SET FOREIGN_KEY_CHECKS = 1;

-- =========================
-- Qket 테스트 데이터
-- 공연장 6개 / 공연 10개 / 좌석 500~2000석
-- =========================

-- ============================================================
-- [테스트 케이스] 모든 계정 비밀번호: test1234
-- ============================================================
--
-- [TC-01] 로그인 성공
--   ID: testuser01 / PW: test1234
--   기대결과: 로그인 성공, 홈 화면 이동
--
-- [TC-02] 잘못된 비밀번호
--   ID: testuser01 / PW: wrong1234
--   기대결과: 로그인 실패 메시지 출력
--
-- [TC-03] 정지된 계정 로그인 시도
--   ID: testuser06 / PW: test1234  (status: SUSPENDED)
--   기대결과: 계정 정지 안내 메시지 출력, 로그인 불가
--
-- [TC-04] 마이페이지 - 예매내역 다건 조회
--   ID: testuser01 로그인 → 마이페이지
--   기대결과: 3건 예매내역 조회
--     · 아이유 콘서트 - The Golden Hour  1회차 (2026-08-15)  A-1  VIP
--     · 임영웅 - 영웅시대               1회차 (2026-10-01)  B-5  R
--     · 뮤지컬 레미제라블               1회차 (2026-08-20)  C-10 R
--
-- [TC-05] 마이페이지 - 예매내역 없는 유저
--   ID: testuser05 로그인 → 마이페이지
--   기대결과: 예매내역 없음 표시
--
-- [TC-06] 좌석 선택 화면 - 이미 예매된 좌석 비활성화 확인
--   아이유 콘서트 1회차(round_id=1) 좌석 선택 화면 진입
--   기대결과: A-1(testuser01 예매), A-2(testuser02 예매) 선택 불가로 표시
--
-- [TC-07] 동일 공연 다른 회차 중복 예매
--   ID: testuser02 로그인 → 마이페이지
--   기대결과: 2건 예매내역 조회
--     · 아이유 콘서트 1회차 (2026-08-15) A-2 VIP
--     · 아이유 콘서트 2회차 (2026-08-16) A-1 VIP  ← 같은 공연 다른 회차
--
-- [TC-08] 여러 공연장 예매 조회
--   ID: testuser04 로그인 → 마이페이지
--   기대결과: 2건 예매내역 조회 (공연장이 서로 다름)
--     · BTS World Tour 1회차 (2026-09-01)  KSPO DOME        A-2 VIP
--     · 세븐틴 - Be The Sun 1회차 (2026-09-05) KSPO DOME    D-20 R
--
-- [TC-09] 예매 취소 후 이력 조회
--   ID: testuser03 로그인 → 마이페이지
--   기대결과: BTS 1회차 A-1 VIP → 최종 상태 CANCELLED로 표시
--             (RESERVATION_HISTORY에 RESERVED→CANCELLED 이력 2건,
--              RESERVATIONS.user_id는 NULL)
--
-- [TC-10] 관리자 계정 로그인
--   ID: admin01 / PW: test1234  (role: ADMIN)
--   기대결과: 관리자 권한으로 로그인
--
-- [TC-11] 매니저 계정 로그인
--   ID: manager01 / PW: test1234  (role: MANAGER)
--   기대결과: 매니저 권한으로 로그인
-- ============================================================

-- ROLES
INSERT INTO ROLES (role_name) VALUES
  ('USER'),
  ('MANAGER'),
  ('ADMIN');

-- VENUE (공연장 6개)
INSERT INTO VENUE (venue_name) VALUES
  ('서울 올림픽공원 체조경기장'),
  ('KSPO DOME'),
  ('잠실실내체육관'),
  ('고척스카이돔'),
  ('블루스퀘어 마스터카드홀'),
  ('인천 파라다이스시티 아레나');

-- CATEGORIES (공연 카테고리 5개)
INSERT INTO CATEGORIES (category_nm, sort_order) VALUES
  ('콘서트', 1),
  ('뮤지컬', 2),
  ('팬미팅', 3),
  ('스포츠', 4),
  ('연극', 5);

-- PERFORMANCES (공연 10개)
-- ins_id/ins_ip: 실제 앱에서는 AdminController.createPerformance() 통해 등록한 관리자 정보가 들어감 →
--   시드 데이터도 그 흐름을 흉내내서 admin01이 등록한 것으로 채움 (ins_ip는 로컬 테스트용 더미값)
-- category_id: 1=콘서트, 2=뮤지컬, 3=팬미팅, 4=스포츠, 5=연극
INSERT INTO PERFORMANCES (p_title, venue_id, category_id, poster_url, ins_id, ins_ip) VALUES
  ('아이유 콘서트 - The Golden Hour',     1, 1, 'https://example.com/poster/iu.jpg',         'admin01', '127.0.0.1'),
  ('BTS World Tour - Yet To Come',       2, 1, 'https://example.com/poster/bts.jpg',        'admin01', '127.0.0.1'),
  ('BLACKPINK - Born Pink',              4, 1, 'https://example.com/poster/blackpink.jpg',  'admin01', '127.0.0.1'),
  ('임영웅 - 영웅시대',                   1, 1, 'https://example.com/poster/lim.jpg',        'admin01', '127.0.0.1'),
  ('뮤지컬 레미제라블',                   5, 2, 'https://example.com/poster/miserable.jpg',  'admin01', '127.0.0.1'),
  ('세븐틴 - Be The Sun',                2, 1, 'https://example.com/poster/seventeen.jpg',  'admin01', '127.0.0.1'),
  ('NewJeans 팬미팅 - Bunnies Camp',     3, 3, 'https://example.com/poster/newjeans.jpg',   'admin01', '127.0.0.1'),
  ('나훈아 - 테스형!',                    4, 1, 'https://example.com/poster/na.jpg',         'admin01', '127.0.0.1'),
  ('뮤지컬 오페라의 유령',                5, 2, 'https://example.com/poster/phantom.jpg',    'admin01', '127.0.0.1'),
  ('태연 - My Voice Concert',            6, 1, 'https://example.com/poster/taeyeon.jpg',    'admin01', '127.0.0.1');

-- USERS (password: test1234)
INSERT INTO USERS (user_id, user_nm, pwd, user_email, role_id, user_status) VALUES
  ('admin01',    '관리자01',    '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'admin01@qket.com',    3, 'ACTIVE'),
  ('manager01',  '매니저01',   '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'manager01@qket.com',  2, 'ACTIVE'),
  ('testuser01', '테스트유저01', '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'testuser01@qket.com', 1, 'ACTIVE'),
  ('testuser02', '테스트유저02', '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'testuser02@qket.com', 1, 'ACTIVE'),
  ('testuser03', '테스트유저03', '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'testuser03@qket.com', 1, 'ACTIVE'),
  ('testuser04', '테스트유저04', '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'testuser04@qket.com', 1, 'ACTIVE'),
  ('testuser05', '테스트유저05', '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'testuser05@qket.com', 1, 'ACTIVE'),
  ('testuser06', '테스트유저06', '$2b$10$hWBKmcDTCeEpTSo69AszSOq83qcpV.y7HJwWtweymXyxLmL7kD4Am', 'testuser06@qket.com', 1, 'SUSPENDED');

-- PERFORMANCE_ROUND
-- [로컬 테스트 편의] open_time을 전부 과거로 당겨서 로컬에서 바로 "예매하기"가 눌리도록 함
-- (round_time = 실제 공연 일시는 원래 값 그대로 유지 — 화면/데이터상 의미는 안 바뀜)
INSERT INTO PERFORMANCE_ROUND (performance_id, round_time, open_time, round_status, ins_id, ins_ip) VALUES
  -- 아이유 (venue=1)
  (1, '2026-08-15 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (1, '2026-08-16 17:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- BTS (venue=2)
  (2, '2026-09-01 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (2, '2026-09-02 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- BLACKPINK (venue=4)
  (3, '2026-09-15 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (3, '2026-09-16 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 임영웅 (venue=1)
  (4, '2026-10-01 18:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (4, '2026-10-02 18:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (4, '2026-10-03 15:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 뮤지컬 레미제라블 (venue=5)
  (5, '2026-08-20 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-21 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-22 14:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 세븐틴 (venue=2)
  (6, '2026-09-05 18:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (6, '2026-09-06 15:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- NewJeans (venue=3)
  (7, '2026-08-30 17:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (7, '2026-08-31 17:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 나훈아 (venue=4)
  (8, '2026-10-10 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (8, '2026-10-11 17:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 뮤지컬 오페라의 유령 (venue=5)
  (9, '2026-09-10 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (9, '2026-09-11 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (9, '2026-09-12 14:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 태연 (venue=6)
  (10, '2026-11-01 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (10, '2026-11-02 17:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),

  -- ── 달력 화면(PER02_DETAIL02) 확인용 추가 회차 (round_id 24~) ──
  -- [주의] 공연별로 묶지 않고 "맨 뒤에" 붙인 이유:
  --   round_id 는 AUTO_INCREMENT 라 중간에 끼워넣으면 그 뒤 회차들의 ID가 전부 밀린다.
  --   아래 시드들이 회차를 ID로 직접 참조하고 있어서(캐스팅 10·11·12, 예매 1·2·3·7·10·13)
  --   중간 삽입은 엉뚱한 회차에 예매/캐스팅이 붙는 결과가 된다. 새 회차는 항상 끝에 추가할 것.
  -- 레미제라블(5): 기존 8/20~22 + 아래 9회차 = 총 12회차, 8월에 흩어져 있고 9월까지 이어짐
  (5, '2026-08-06 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-07 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-13 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-14 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-22 19:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'), -- 8/22 하루 2회차(마티네+저녁)
  (5, '2026-08-27 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-08-28 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-09-03 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (5, '2026-09-04 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  -- 오페라의 유령(9): 기존 9/10~12 + 아래 5회차 = 총 8회차, 9월→10월 달 이동 확인용
  (9, '2026-09-17 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (9, '2026-09-18 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (9, '2026-09-19 14:00:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (9, '2026-10-01 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1'),
  (9, '2026-10-02 19:30:00', '2025-01-01 10:00:00', 'OPEN', 'admin01', '127.0.0.1');

-- =========================
-- PERFORMANCE_CAST (공연 캐스팅) — PER02_DETAIL01 공연 상세 조회용
--
-- round_id NULL     = 전체 회차 공통 캐스팅
-- round_id 값 있음   = 해당 회차 전용 (뮤지컬 더블/트리플 캐스팅)
-- casting_nm NULL   = 배역 개념이 없는 공연(콘서트 등)
--
-- [화면 테스트 케이스]
--  · 레미제라블(5)      : 공통 캐스팅 + 회차별 더블 캐스팅이 섞인 경우
--  · 오페라의 유령(9)    : 전체 공통 캐스팅만 있는 경우
--  · 아이유 콘서트(1)    : 배역명이 없는 경우(casting_nm NULL) + 특정 회차 게스트
--  · 나머지 공연         : 캐스팅이 아예 없는 경우(빈 목록 UI 확인용)
--
-- 회차 ID 참고: 공연5 → 10,11,12 / 공연9 → 19,20,21 / 공연1 → 1,2
-- =========================
INSERT INTO PERFORMANCE_CAST (performance_id, round_id, actor_nm, casting_nm, sort_order, ins_id, ins_ip) VALUES
  -- 뮤지컬 레미제라블 (공연 5) — 장발장/자베르는 회차별 캐스팅, 나머지 배역은 전체 공통
  (5, 10,   '김민석', '장발장',     0, 'admin01', '127.0.0.1'),
  (5, 11,   '이서준', '장발장',     0, 'admin01', '127.0.0.1'),
  (5, 12,   '김민석', '장발장',     0, 'admin01', '127.0.0.1'),
  (5, 10,   '박도현', '자베르',     1, 'admin01', '127.0.0.1'),
  (5, 11,   '박도현', '자베르',     1, 'admin01', '127.0.0.1'),
  (5, 12,   '강태영', '자베르',     1, 'admin01', '127.0.0.1'),
  (5, NULL, '한지우', '판틴',       2, 'admin01', '127.0.0.1'),
  (5, NULL, '윤소희', '코제트',     3, 'admin01', '127.0.0.1'),
  (5, NULL, '최현우', '마리우스',   4, 'admin01', '127.0.0.1'),
  (5, NULL, '오세영', '테나르디에', 5, 'admin01', '127.0.0.1'),

  -- 뮤지컬 오페라의 유령 (공연 9) — 전체 회차 공통 캐스팅만
  (9, NULL, '서지훈', '팬텀',       0, 'admin01', '127.0.0.1'),
  (9, NULL, '임하늘', '크리스틴',   1, 'admin01', '127.0.0.1'),
  (9, NULL, '정우진', '라울',       2, 'admin01', '127.0.0.1'),
  (9, NULL, '문가영', '칼롯타',     3, 'admin01', '127.0.0.1'),

  -- 아이유 콘서트 (공연 1) — 배역명 없음(NULL), 2회차에만 게스트 출연
  (1, NULL, '아이유', NULL,         0, 'admin01', '127.0.0.1'),
  (1, 2,    '이하은', NULL,         1, 'admin01', '127.0.0.1');

-- =========================
-- SEATS
-- 숫자 생성: a(1-10) x b(0,10,20,...) CROSS JOIN 으로 1-N 시퀀스 생성
-- 행(row): CHAR(64+n) → A,B,C,...
-- 등급: 1-3행 VIP / 4-10행 R / 나머지 S
-- =========================

-- venue 1: 서울 올림픽공원 체조경기장 (A-T행=20행, 1-50열=50열 → 1000석)
INSERT INTO SEATS (venue_id, seat_row, seat_colume, grade)
SELECT 1, CHAR(64 + r.n), c.n,
  CASE WHEN r.n <= 3 THEN 'VIP' WHEN r.n <= 10 THEN 'R' ELSE 'S' END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
      UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) r
CROSS JOIN
  (SELECT a.n + b.n AS n
   FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
         UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a
   CROSS JOIN (SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40) b
   WHERE a.n + b.n <= 50) c;

-- venue 2: KSPO DOME (A-T행=20행, 1-60열=60열 → 1200석)
INSERT INTO SEATS (venue_id, seat_row, seat_colume, grade)
SELECT 2, CHAR(64 + r.n), c.n,
  CASE WHEN r.n <= 3 THEN 'VIP' WHEN r.n <= 10 THEN 'R' ELSE 'S' END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
      UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) r
CROSS JOIN
  (SELECT a.n + b.n AS n
   FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
         UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a
   CROSS JOIN (SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40 UNION ALL SELECT 50) b
   WHERE a.n + b.n <= 60) c;

-- venue 3: 잠실실내체육관 (A-P행=16행, 1-50열=50열 → 800석)
INSERT INTO SEATS (venue_id, seat_row, seat_colume, grade)
SELECT 3, CHAR(64 + r.n), c.n,
  CASE WHEN r.n <= 2 THEN 'VIP' WHEN r.n <= 8 THEN 'R' ELSE 'S' END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
      UNION ALL SELECT 16) r
CROSS JOIN
  (SELECT a.n + b.n AS n
   FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
         UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a
   CROSS JOIN (SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40) b
   WHERE a.n + b.n <= 50) c;

-- venue 4: 고척스카이돔 (A-Y행=25행, 1-80열=80열 → 2000석)
INSERT INTO SEATS (venue_id, seat_row, seat_colume, grade)
SELECT 4, CHAR(64 + r.n), c.n,
  CASE WHEN r.n <= 3 THEN 'VIP' WHEN r.n <= 12 THEN 'R' ELSE 'S' END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
      UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
      UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25) r
CROSS JOIN
  (SELECT a.n + b.n AS n
   FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
         UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a
   CROSS JOIN (SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40
               UNION ALL SELECT 50 UNION ALL SELECT 60 UNION ALL SELECT 70) b
   WHERE a.n + b.n <= 80) c;

-- venue 5: 블루스퀘어 마스터카드홀 (A-J행=10행, 1-50열=50열 → 500석)
INSERT INTO SEATS (venue_id, seat_row, seat_colume, grade)
SELECT 5, CHAR(64 + r.n), c.n,
  CASE WHEN r.n <= 2 THEN 'VIP' WHEN r.n <= 6 THEN 'R' ELSE 'S' END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) r
CROSS JOIN
  (SELECT a.n + b.n AS n
   FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
         UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a
   CROSS JOIN (SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40) b
   WHERE a.n + b.n <= 50) c;

-- venue 6: 인천 파라다이스시티 아레나 (A-O행=15행, 1-60열=60열 → 900석)
INSERT INTO SEATS (venue_id, seat_row, seat_colume, grade)
SELECT 6, CHAR(64 + r.n), c.n,
  CASE WHEN r.n <= 2 THEN 'VIP' WHEN r.n <= 8 THEN 'R' ELSE 'S' END
FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
      UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15) r
CROSS JOIN
  (SELECT a.n + b.n AS n
   FROM (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
         UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10) a
   CROSS JOIN (SELECT 0 n UNION ALL SELECT 10 UNION ALL SELECT 20 UNION ALL SELECT 30 UNION ALL SELECT 40 UNION ALL SELECT 50) b
   WHERE a.n + b.n <= 60) c;

-- =========================
-- RESERVATIONS
-- 각 공연의 venue_id와 같은 venue의 좌석 x 해당 공연 회차 조합
-- =========================
-- 실제 앱에서는 회차 등록 시 PerformanceMapper.initReservationSlots() 가 이 슬롯들을 만듦 (행위자 = 회차를 등록한 관리자)
INSERT INTO RESERVATIONS (user_id, seat_id, round_id, performance_id, reserved_status, ins_id, ins_ip)
SELECT NULL, s.seat_id, r.round_id, r.performance_id, 'AVAILABLE', 'admin01', '127.0.0.1'
FROM SEATS s
CROSS JOIN PERFORMANCE_ROUND r
INNER JOIN PERFORMANCES p ON r.performance_id = p.performance_id
WHERE p.venue_id = s.venue_id;

-- ============================================================
-- 데모용 예매 데이터
-- ============================================================

-- upt_id/upt_ip: 실제로는 ReservationServiceImpl.reserve() 가 예매한 본인 아이디로 채움 → 시드도 동일하게 맞춤

-- [TC-04 / TC-06] testuser01: 아이유 1회차 A-1 VIP
UPDATE RESERVATIONS SET user_id = 'testuser01', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser01', upt_ip = '127.0.0.1'
WHERE round_id = 1 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='A' AND seat_colume='1')
  AND user_id IS NULL;

-- [TC-04] testuser01: 임영웅 1회차 B-5 R
UPDATE RESERVATIONS SET user_id = 'testuser01', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser01', upt_ip = '127.0.0.1'
WHERE round_id = 7 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='B' AND seat_colume='5')
  AND user_id IS NULL;

-- [TC-04] testuser01: 뮤지컬 레미제라블 1회차 C-10 R
UPDATE RESERVATIONS SET user_id = 'testuser01', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser01', upt_ip = '127.0.0.1'
WHERE round_id = 10 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=5 AND seat_row='C' AND seat_colume='10')
  AND user_id IS NULL;

-- [TC-06 / TC-07] testuser02: 아이유 1회차 A-2 VIP
UPDATE RESERVATIONS SET user_id = 'testuser02', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser02', upt_ip = '127.0.0.1'
WHERE round_id = 1 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='A' AND seat_colume='2')
  AND user_id IS NULL;

-- [TC-07] testuser02: 아이유 2회차 A-1 VIP (동일 공연 다른 회차)
UPDATE RESERVATIONS SET user_id = 'testuser02', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser02', upt_ip = '127.0.0.1'
WHERE round_id = 2 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='A' AND seat_colume='1')
  AND user_id IS NULL;

-- [TC-09] testuser03: BTS 1회차 A-1 → 예매 후 취소 (RESERVATIONS는 NULL 유지)

-- [TC-08] testuser04: BTS 1회차 A-2 VIP
UPDATE RESERVATIONS SET user_id = 'testuser04', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser04', upt_ip = '127.0.0.1'
WHERE round_id = 3 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=2 AND seat_row='A' AND seat_colume='2')
  AND user_id IS NULL;

-- [TC-08] testuser04: 세븐틴 1회차 D-20 R (다른 공연장, 같은 venue_id=2)
UPDATE RESERVATIONS SET user_id = 'testuser04', reserved_status = 'RESERVED', reserved_at = NOW(), upt_id = 'testuser04', upt_ip = '127.0.0.1'
WHERE round_id = 13 AND seat_id = (SELECT seat_id FROM SEATS WHERE venue_id=2 AND seat_row='D' AND seat_colume='20')
  AND user_id IS NULL;

-- [TC-05] testuser05: 예매내역 없음 (UPDATE 없음)

-- ============================================================
-- RESERVATION_HISTORY (마이페이지 조회 기준 테이블)
-- RESERVATIONS UPDATE와 반드시 쌍으로 관리
-- action='RESERVED' → RESERVATIONS.user_id 있음
-- action='CANCELLED' → RESERVATIONS.user_id=NULL
-- ============================================================

-- ins_id/ins_ip: 실제로는 ReservationServiceImpl 이 예매/취소한 본인 아이디로 채움 → 시드도 동일하게 맞춤 (user_id 값과 항상 같음)

-- [TC-04] testuser01: 아이유 1회차 A-1 VIP
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser01', (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='A' AND seat_colume='1'),
       1, performance_id, 'RESERVED', 'testuser01', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 1;

-- [TC-04] testuser01: 임영웅 1회차 B-5 R
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser01', (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='B' AND seat_colume='5'),
       7, performance_id, 'RESERVED', 'testuser01', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 7;

-- [TC-04] testuser01: 뮤지컬 레미제라블 1회차 C-10 R
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser01', (SELECT seat_id FROM SEATS WHERE venue_id=5 AND seat_row='C' AND seat_colume='10'),
       10, performance_id, 'RESERVED', 'testuser01', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 10;

-- [TC-07] testuser02: 아이유 1회차 A-2 VIP
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser02', (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='A' AND seat_colume='2'),
       1, performance_id, 'RESERVED', 'testuser02', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 1;

-- [TC-07] testuser02: 아이유 2회차 A-1 VIP
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser02', (SELECT seat_id FROM SEATS WHERE venue_id=1 AND seat_row='A' AND seat_colume='1'),
       2, performance_id, 'RESERVED', 'testuser02', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 2;

-- [TC-09] testuser03: BTS 1회차 A-1 → RESERVED 후 CANCELLED (RESERVATIONS는 NULL)
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser03', (SELECT seat_id FROM SEATS WHERE venue_id=2 AND seat_row='A' AND seat_colume='1'),
       3, performance_id, 'RESERVED', 'testuser03', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 3;

INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser03', (SELECT seat_id FROM SEATS WHERE venue_id=2 AND seat_row='A' AND seat_colume='1'),
       3, performance_id, 'CANCELLED', 'testuser03', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 3;

-- [TC-08] testuser04: BTS 1회차 A-2 VIP
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser04', (SELECT seat_id FROM SEATS WHERE venue_id=2 AND seat_row='A' AND seat_colume='2'),
       3, performance_id, 'RESERVED', 'testuser04', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 3;

-- [TC-08] testuser04: 세븐틴 1회차 D-20 R
INSERT INTO RESERVATION_HISTORY (user_id, seat_id, round_id, performance_id, action, ins_id, ins_ip)
SELECT 'testuser04', (SELECT seat_id FROM SEATS WHERE venue_id=2 AND seat_row='D' AND seat_colume='20'),
       13, performance_id, 'RESERVED', 'testuser04', '127.0.0.1' FROM PERFORMANCE_ROUND WHERE round_id = 13;

-- ============================================================
-- PROGRAMS / ROLE_PROGRAMS / MENUS (권한 확장 · 프로그램관리 · 메뉴관리)
-- 지금 SiteNav.tsx / 각 페이지에 하드코딩돼 있는 role 체크를 DB 기반으로 옮기기 위한 시드
-- ============================================================

INSERT INTO PROGRAMS (program_nm, url_path, program_type, ins_id, ins_ip) VALUES
  ('공연 목록',     '/',               'MENU', 'SYSTEM', '127.0.0.1'),
  ('마이페이지',    '/mypage',         'MENU', 'SYSTEM', '127.0.0.1'),
  ('공연 관리',     '/performances',   'MENU', 'SYSTEM', '127.0.0.1'),
  ('공연 등록',     '/performances/new','PAGE', 'SYSTEM', '127.0.0.1'),
  ('사용자 관리',   '/admin/users',    'MENU', 'SYSTEM', '127.0.0.1'),
  ('프로그램관리',  '/admin/programs', 'MENU', 'SYSTEM', '127.0.0.1'),
  ('메뉴관리',      '/admin/menus',    'MENU', 'SYSTEM', '127.0.0.1'),
  ('카테고리관리',  '/admin/categories','MENU', 'SYSTEM', '127.0.0.1');

-- ROLE_PROGRAMS: 1=USER, 2=MANAGER, 3=ADMIN
INSERT INTO ROLE_PROGRAMS (role_id, program_id, ins_id, ins_ip)
SELECT r.role_id, p.program_id, 'SYSTEM', '127.0.0.1'
FROM ROLES r CROSS JOIN PROGRAMS p
WHERE (p.url_path IN ('/', '/mypage'))                                    -- 전체 role 공통
   OR (p.url_path IN ('/performances', '/performances/new') AND r.role_id IN (2, 3))  -- 매니저/관리자
   OR (p.url_path IN ('/admin/users', '/admin/programs', '/admin/menus', '/admin/categories') AND r.role_id = 3);  -- 관리자만

-- MENUS: 현재 SiteNav.tsx 순서 그대로, 등록 페이지는 메뉴 미노출
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT program_id, NULL, '공연', 1, 'SYSTEM', '127.0.0.1' FROM PROGRAMS WHERE url_path = '/';
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT program_id, NULL, '마이페이지', 2, 'SYSTEM', '127.0.0.1' FROM PROGRAMS WHERE url_path = '/mypage';
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT program_id, NULL, '공연 관리', 3, 'SYSTEM', '127.0.0.1' FROM PROGRAMS WHERE url_path = '/performances';
-- '관리자'는 program_id가 NULL인 "그룹 전용" 메뉴 — 연결된 페이지 없이 하위메뉴만 묶어서
-- SiteNav에서 마우스 호버 시 드롭다운으로 노출하는 용도 (클릭해서 이동할 자기 자신의 페이지는 없음)
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip) VALUES
  (NULL, NULL, '관리자', 4, 'SYSTEM', '127.0.0.1');
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT p.program_id, m.menu_id, '사용자관리', 1, 'SYSTEM', '127.0.0.1'
FROM PROGRAMS p, MENUS m WHERE p.url_path = '/admin/users' AND m.menu_nm = '관리자';
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT p.program_id, m.menu_id, '프로그램관리', 2, 'SYSTEM', '127.0.0.1'
FROM PROGRAMS p, MENUS m WHERE p.url_path = '/admin/programs' AND m.menu_nm = '관리자';
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT p.program_id, m.menu_id, '메뉴관리', 3, 'SYSTEM', '127.0.0.1'
FROM PROGRAMS p, MENUS m WHERE p.url_path = '/admin/menus' AND m.menu_nm = '관리자';
INSERT INTO MENUS (program_id, parent_menu_id, menu_nm, sort_order, ins_id, ins_ip)
SELECT p.program_id, m.menu_id, '카테고리관리', 4, 'SYSTEM', '127.0.0.1'
FROM PROGRAMS p, MENUS m WHERE p.url_path = '/admin/categories' AND m.menu_nm = '관리자';
