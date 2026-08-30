package com.exam.queue.service.impl;

import com.exam.queue.domain.QueueStatus;
import com.exam.queue.domain.QueueTokenInfo;
import com.exam.queue.dto.QueueJoinResponse;
import com.exam.queue.dto.QueueStatusResponse;
import com.exam.queue.repository.RedisQueueRepository;
import com.exam.queue.service.QueueService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 *
 * 파일명: QueueServiceImpl.java
 *
 **/

@Service
public class QueueServiceImpl implements QueueService {

        // 2000명 부하테스트 기준 값 — 슬롯 즉시반납 로직(예매 성공/포기/매진/결제실패 시 leave())과
        // 같이 넣어서 400까지 상향(backend 8replica가 실패 0건으로 버텨낸 걸 확인). 다음 부하테스트로
        // queue_wait_seconds가 실제로 줄었는지 재측정 필요.
        private static final int MAX_ACTIVE_USERS = 400;

        // 대기시간 예상(estimatedWait) 계산용 — 아래 getStatus() 참고
        private static final long AVG_WAVE_SECONDS = 30;

        private static final Duration WAITING_TTL = Duration.ofMinutes(30);

        // 슬롯 즉시반납 로직이 들어온 뒤로는 이 TTL이 처리량을 좌우하지 않고, beforeunload가 안 뜨는
        // 경우(강제종료, 네트워크 끊김)에 대한 안전망 역할 — 결제위젯 소요시간을 안 침범할 정도로 5분.
        private static final Duration ACTIVE_TTL = Duration.ofMinutes(5);

        private final RedisQueueRepository repository;

        public QueueServiceImpl(
                        RedisQueueRepository repository) {
                this.repository = repository;
        }

        /***********************************
         * 이름 : join
         * 기능 : 대기열 입장 ( 토큰 조회 및 생성 )
         * param : Long, String
         * return : QueueJoinResponse
         ************************************/
        @Override
        public QueueJoinResponse join(
                        Long scheduleId,
                        String userId) {
                String existingToken = repository.getUserToken(scheduleId, userId);

                if (existingToken != null) {
                        Optional<QueueTokenInfo> existingInfo = repository.findToken(existingToken);

                        if (existingInfo.isPresent()) {
                                return new QueueJoinResponse(existingToken);
                        }
                }

                String token = UUID.randomUUID().toString();

                repository.saveToken(
                                token,
                                scheduleId,
                                userId,
                                WAITING_TTL);

                boolean claimed = repository.claimUserToken(
                                scheduleId,
                                userId,
                                token,
                                WAITING_TTL);

                if (!claimed) {
                        repository.deleteToken(token);

                        return new QueueJoinResponse(
                                        repository.getUserToken(scheduleId, userId));
                }

                repository.addWaiting(
                                scheduleId,
                                token,
                                System.currentTimeMillis());

                return new QueueJoinResponse(token);
        }

        /***********************************
         * 이름 : getStatus
         * 기능 : 대기열 토큰 상태 조회
         * param : String, String
         * return : QueueStatusResponse
         ************************************/
        @Override
        public QueueStatusResponse getStatus(
                        String queueToken,
                        String userId) {
                Optional<QueueTokenInfo> optionalInfo = repository.findToken(queueToken);

                if (optionalInfo.isEmpty()) {
                        return expired(queueToken);
                }

                QueueTokenInfo tokenInfo = optionalInfo.get();

                validateOwner(tokenInfo, userId);

                admitAvailableUsers(tokenInfo.scheduleId());

                Long expiresAt = repository.getActiveExpiresAt(
                                tokenInfo.scheduleId(),
                                queueToken);

                if (expiresAt != null) {
                        // 좌석 선택 화면 카운트다운용 잔여 시간(초). 음수가 나오지 않게 0으로 바닥을 깔아둠 —
                        // 만료 직후 removeExpiredActive가 아직 안 돈 찰나에 여기 걸릴 수 있음.
                        long remainingSeconds = Math.max(
                                        0,
                                        (expiresAt - System.currentTimeMillis()) / 1000);

                        return new QueueStatusResponse(
                                        queueToken,
                                        QueueStatus.ENTERED,
                                        0,
                                        0,
                                        remainingSeconds);
                }

                Long rank = repository.getWaitingRank(
                                tokenInfo.scheduleId(),
                                queueToken);

                if (rank == null) {
                        removeToken(queueToken, tokenInfo);
                        return expired(queueToken);
                }

                // rank*3(3초마다 1명 입장 가정)은 MAX_ACTIVE_USERS만큼 한 번에 몰아서 입장시키는 구조와
                // 안 맞아서 순번을 웨이브 단위로 환산 — "앞으로 웨이브가 몇 번 더 돌아야 하는지 ×
                // 웨이브 평균 시간". AVG_WAVE_SECONDS는 관찰 기반 근사치, 실측 데이터 쌓이면 조정 필요.
                long waves = (rank + MAX_ACTIVE_USERS - 1) / MAX_ACTIVE_USERS;
                long estimatedWait = Math.max(3, waves * AVG_WAVE_SECONDS);

                return new QueueStatusResponse(
                                queueToken,
                                QueueStatus.WAITING,
                                rank,
                                estimatedWait,
                                0);
        }

        /***********************************
         * 이름 : leave
         * 기능 : 대기 목록 및 활성 목록 제거 ( 관련 토큰 제거 )
         * param : String, String
         * return : void
         ************************************/
        @Override
        public void leave(
                        String queueToken,
                        String userId) {
                Optional<QueueTokenInfo> optionalInfo = repository.findToken(queueToken);

                if (optionalInfo.isEmpty()) {
                        return;
                }

                QueueTokenInfo tokenInfo = optionalInfo.get();

                validateOwner(tokenInfo, userId);

                repository.removeWaiting(
                                tokenInfo.scheduleId(),
                                queueToken);

                repository.removeActive(
                                tokenInfo.scheduleId(),
                                queueToken);

                removeToken(queueToken, tokenInfo);
        }

        /***********************************
         * 이름 : canEnter
         * 기능 : 예매 자격 조회 ( 만료된 활성 사용자 제거 )
         * param : Long, String, String
         * return : boolean
         ************************************/
        @Override
        public boolean canEnter(
                        Long scheduleId,
                        String queueToken,
                        String userId) {
                Optional<QueueTokenInfo> optionalInfo = repository.findToken(queueToken);

                if (optionalInfo.isEmpty()) {
                        return false;
                }

                QueueTokenInfo tokenInfo = optionalInfo.get();

                if (!tokenInfo.scheduleId().equals(scheduleId)) {
                        return false;
                }

                if (!tokenInfo.userId().equals(userId)) {
                        return false;
                }

                repository.removeExpiredActive(
                                scheduleId,
                                System.currentTimeMillis());

                return repository.isActive(
                                scheduleId,
                                queueToken);
        }

        /***********************************
         * 이름 : admitAvailableUsers
         * 기능 : 동시 접속 인원 제한, 빈자리 발생 시 대기 순서대로 입장시키는 기능
         * param : Long
         * return : void
         ************************************/
        private void admitAvailableUsers(Long scheduleId) {
                String lockOwner = UUID.randomUUID().toString();

                boolean locked = repository.acquireAdmissionLock(
                                scheduleId,
                                lockOwner,
                                Duration.ofSeconds(5));

                if (!locked) {
                        return;
                }

                try {
                        long now = System.currentTimeMillis();

                        repository.removeExpiredActive(
                                        scheduleId,
                                        now);

                        long activeCount = repository.getActiveCount(scheduleId);

                        long available = MAX_ACTIVE_USERS - activeCount;

                        while (available > 0) {
                                Set<String> waitingTokens = repository.getFirstWaiting(
                                                scheduleId,
                                                available);

                                if (waitingTokens == null ||
                                                waitingTokens.isEmpty()) {
                                        break;
                                }

                                boolean processed = false;

                                for (String token : waitingTokens) {
                                        Optional<QueueTokenInfo> tokenInfo = repository.findToken(token);

                                        // 만료된 대기 토큰 제거
                                        if (tokenInfo.isEmpty()) {
                                                repository.removeWaiting(
                                                                scheduleId,
                                                                token);
                                                processed = true;
                                                continue;
                                        }

                                        long expiresAt = now + ACTIVE_TTL.toMillis();

                                        boolean moved = repository.moveToActive(
                                                        scheduleId,
                                                        token,
                                                        expiresAt);

                                        if (moved) {
                                                repository.refreshToken(
                                                                token,
                                                                ACTIVE_TTL);

                                                repository.refreshUserToken(
                                                                scheduleId,
                                                                tokenInfo.get().userId(),
                                                                ACTIVE_TTL);

                                                available--;
                                                processed = true;
                                        }
                                }

                                if (!processed) {
                                        break;
                                }
                        }
                } finally {
                        repository.releaseAdmissionLock(
                                        scheduleId,
                                        lockOwner);
                }
        }

        /***********************************
         * 이름 : validateOwner
         * 기능 : 대기열 토큰 소유자 검증 기능
         * param : QueueTokenInfo,String,
         * return : void
         ************************************/
        private void validateOwner(
                        QueueTokenInfo tokenInfo,
                        String userId) {
                if (!tokenInfo.userId().equals(userId)) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "다른 사용자의 대기열 토큰입니다.");
                }
        }

        /***********************************
         * 이름 : removeToken
         * 기능 : 대기열 토큰 삭제 기능
         * param : QueueTokenInfo,String
         * return : void
         ************************************/
        private void removeToken(
                        String queueToken,
                        QueueTokenInfo tokenInfo) {
                repository.deleteToken(queueToken);

                repository.deleteUserToken(
                                tokenInfo.scheduleId(),
                                tokenInfo.userId(),
                                queueToken);
        }

        /***********************************
         * 이름 : expired
         * 기능 : 만료된 대기열 상태 응답 생성
         * param : String
         * return : QueueStatusResponse
         ************************************/
        private QueueStatusResponse expired(String token) {
                return new QueueStatusResponse(
                                token,
                                QueueStatus.EXPIRED,
                                0,
                                0,
                                0);
        }
}