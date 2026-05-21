DELETE FROM reservation;
DELETE FROM reservation_time;
DELETE FROM theme;
DELETE FROM store;
DELETE FROM member;

ALTER TABLE reservation ALTER COLUMN id RESTART WITH 1;
ALTER TABLE reservation_time ALTER COLUMN id RESTART WITH 1;
ALTER TABLE theme ALTER COLUMN id RESTART WITH 1;
ALTER TABLE store ALTER COLUMN id RESTART WITH 1;
ALTER TABLE member ALTER COLUMN id RESTART WITH 1;

-- 1. 회원 데이터 생성 (외래키 참조를 위해 최상단 위치)
INSERT INTO member (login_id, password, name) VALUES ('jinriro', '1234', '진리로');
INSERT INTO member (login_id, password, name) VALUES ('rice', '1234', '김가현');

-- 2. 예약 시간 데이터 생성
INSERT INTO reservation_time (start_at) VALUES
('10:00:00'), ('11:00:00'), ('12:00:00'), ('13:00:00'), ('14:00:00'),
('15:00:00'), ('16:00:00'), ('17:00:00'), ('18:00:00'), ('19:00:00');

-- 3. 매장 데이터 생성
INSERT INTO store (name, description) VALUES
('강남점', '강남 중심부에 위치한 메인 매장입니다.'),
('홍대점', '홍익대학교 인근에 위치한 매장입니다.'),
('신촌점', '신촌 번화가에 위치한 매장입니다.');

-- 4. 테마 데이터 생성 (store_id 포함)
-- 강남점(1): 테마 1~5 / 홍대점(2): 테마 6~10 / 신촌점(3): 테마 11~15
INSERT INTO theme (name, description, thumbnail_url, store_id) VALUES
('우주선 탈출',        '고장 난 우주선에서 제한 시간 안에 탈출하세요.',             'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=600',  1),
('좀비 아포칼립스',    '봉쇄된 도시에서 생존 키트를 찾아 탈출해야 합니다.',         'https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=600',  1),
('고대 피라미드',      '피라미드 깊숙한 곳의 비밀 방을 열어 보물을 찾으세요.',       'https://images.unsplash.com/photo-1503177119275-0aa32b3a9368?w=600',  1),
('마법학교의 비밀',    '사라진 마법서를 찾아 학교의 저주를 풀어야 합니다.',           'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=600',  1),
('해적선의 보물',      '해적선 선장의 단서를 모아 숨겨진 보물창고를 여세요.',         'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=600',     1),
('미스터리 연구소',    '폐쇄된 연구소에서 실험 기록을 복구하고 탈출하세요.',          'https://images.unsplash.com/photo-1532187863486-abf9dbad1b69?w=600',  2),
('시간여행자',         '뒤틀린 시간 장치를 복구해 현재로 돌아오세요.',               'https://images.unsplash.com/photo-1501139083538-0139583c060f?w=600',  2),
('유령의 저택',        '밤이 끝나기 전 저택의 원혼을 달래는 의식을 완성하세요.',      'https://images.unsplash.com/photo-1509557965875-b88c97052f0e?w=600',  2),
('사라진 화가의 작품', '실종된 화가가 남긴 암호를 풀어 진짜 작품을 찾으세요.',       'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=600',  2),
('심해 탐험',          '산소가 떨어지기 전에 심해 기지의 전원을 복구해야 합니다.',    'https://images.unsplash.com/photo-1518020382113-a7e8fc38eac9?w=600',  2),
('왕실 음모',          '왕궁에서 벌어진 음모의 증거를 찾아 누명을 벗기세요.',         'https://images.unsplash.com/photo-1551698618-1dfe5d97d256?w=600',    3),
('폐병원 탈출',        '버려진 병원에서 수상한 흔적을 추적해 출구를 찾으세요.',       'https://images.unsplash.com/photo-1516574187841-cb9cc2ca948b?w=600',  3),
('한밤중의 서커스',    '멈춰버린 서커스 공연의 비밀을 밝히고 무대를 탈출하세요.',     'https://images.unsplash.com/photo-1576872381149-7847515ce5d8?w=600',  3),
('비밀 요원 작전',     '이중 잠금 장치를 해제하고 기밀 문서를 회수하세요.',           'https://images.unsplash.com/photo-1453873531674-2151bcd01707?w=600',  3),
('드래곤의 동굴',      '드래곤이 잠든 사이 고대 룬을 해독해 동굴을 빠져나오세요.',    'https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=600',  3);

-- 4. 예약 데이터 생성 (name 컬럼 제거 후 member_id 적용)
INSERT INTO reservation (member_id, reservation_date, time_id, theme_id, status) VALUES

-- (진리로 유저 = member_id: 1)
(1, '2026-05-14', 1, 1,  'RESERVED'),
(1, '2026-05-14', 7, 2,  'RESERVED'),
(1, '2026-05-14', 8, 3,  'RESERVED'),
(1, '2026-05-14', 9, 4,  'RESERVED'),

-- 최근 7일 이내 (김민수, 정하늘 등 다른 회원이 아직 없으므로 테스트 편의상 member_id 1 혹은 2로 임시 통합 배치)
(2, '2026-05-13', 1, 1,  'RESERVED'), -- 김가현(2)으로 대체
(1, '2026-05-13', 5, 5,  'RESERVED'),
(1, '2026-05-12', 2, 1,  'RESERVED'),
(2, '2026-05-12', 4, 4,  'RESERVED'),
(1, '2026-05-12', 5, 5,  'RESERVED'),

(1, '2026-05-11', 3, 1,  'RESERVED'),
(2, '2026-05-10', 4, 4,  'CANCELED'),
(2, '2026-05-09', 1, 1,  'RESERVED'),
(1, '2026-05-08', 2, 2,  'RESERVED'),
(1, '2026-05-07', 3, 3,  'RESERVED'),

-- 7일 이전 (과거)
(2, '2026-05-05', 1, 1,  'RESERVED'),
(1, '2026-05-05', 2, 2,  'RESERVED'),
(1, '2026-05-05', 3, 3,  'RESERVED'),
(2, '2026-05-05', 4, 4,  'RESERVED'),
(1, '2026-05-05', 5, 5,  'CANCELED'),
(1, '2026-05-05', 10, 5, 'RESERVED'),
(1, '2026-05-04', 1, 6,  'RESERVED'),
(2, '2026-05-04', 2, 7,  'CANCELED'),
(1, '2026-05-04', 3, 8,  'RESERVED'),
(1, '2026-05-04', 4, 9,  'RESERVED'),
(2, '2026-05-04', 5, 10, 'CANCELED'),
(1, '2026-05-03', 1, 11, 'RESERVED'),
(1, '2026-05-03', 2, 12, 'RESERVED'),
(1, '2026-05-03', 3, 13, 'RESERVED'),
(2, '2026-05-03', 4, 14, 'CANCELED'),
(1, '2026-05-03', 5, 15, 'RESERVED'),
(1, '2026-05-02', 1, 2,  'RESERVED'),
(1, '2026-05-01', 2, 4,  'RESERVED'),
(1, '2026-04-30', 3, 6,  'RESERVED'),
(1, '2026-04-29', 4, 8,  'RESERVED'),
(2, '2026-04-29', 5, 10, 'CANCELED'),
(1, '2026-04-28', 1, 3,  'RESERVED'),
(1, '2026-04-26', 2, 5,  'RESERVED'),
(1, '2026-04-24', 3, 7,  'RESERVED'),
(1, '2026-04-21', 4, 9,  'RESERVED'),
(1, '2026-04-16', 5, 11, 'RESERVED'),
(1, '2026-04-06', 1, 13, 'RESERVED'),
(1, '2026-03-22', 2, 15, 'RESERVED'),

-- 미래 예약
(2, '2026-05-24', 7, 7,  'RESERVED'),
(1, '2026-05-23', 6, 6,  'RESERVED'),
(1, '2026-05-22', 5, 4,  'RESERVED'),
(2, '2026-05-21', 4, 5,  'RESERVED'),
(1, '2026-05-21', 2, 3,  'RESERVED'),
(2, '2026-05-20', 1, 1,  'RESERVED'),
(1, '2026-05-20', 3, 2,  'RESERVED'),

-- 6월
(2, '2026-06-13', 1, 1,  'RESERVED'),
(1, '2026-06-13', 5, 5,  'RESERVED'),
(1, '2026-06-12', 2, 1,  'RESERVED'),
(2, '2026-06-12', 4, 4,  'RESERVED'),
(1, '2026-06-12', 5, 5,  'RESERVED'),
(1, '2026-06-11', 3, 1,  'RESERVED'),
(2, '2026-06-10', 4, 4,  'RESERVED'),
(2, '2026-06-09', 1, 1,  'RESERVED'),
(1, '2026-06-08', 2, 2,  'RESERVED'),
(1, '2026-06-07', 3, 3,  'RESERVED');
