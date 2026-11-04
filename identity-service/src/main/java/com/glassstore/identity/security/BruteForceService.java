package com.glassstore.identity.security;

import com.glassstore.identity.entity.User;
import com.glassstore.identity.exception.AccountLockedException;
import com.glassstore.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 15;

    private final UserRepository userRepository;

    // Concurrency control per key (email / username / IP)
    private final ConcurrentHashMap<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    private ReentrantLock getLockFor(String key) {
        return userLocks.computeIfAbsent(key.toLowerCase().trim(), k -> new ReentrantLock());
    }

    /**
     * Kiểm tra trạng thái khóa của tài khoản trước khi thực hiện xác thực.
     */
    public void checkIfLocked(User user) {
        String key = user.getEmail() != null ? user.getEmail() : user.getUsername();
        ReentrantLock lock = getLockFor(key);
        lock.lock();
        try {
            LocalDateTime lockoutExpiration = user.getLockoutExpiration();
            if (lockoutExpiration != null) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(lockoutExpiration)) {
                    long minutesLeft = Duration.between(now, lockoutExpiration).toMinutes() + 1;
                    throw new AccountLockedException("Tài khoản đã bị tạm khóa trong " + LOCK_DURATION_MINUTES 
                            + " phút do nhập sai mật khẩu " + MAX_ATTEMPTS + " lần liên tiếp. Vui lòng thử lại sau " 
                            + minutesLeft + " phút!");
                } else {
                    // Đã hết thời gian 15 phút -> Tự động mở khóa
                    user.setFailedAttempts(0);
                    user.setLockoutExpiration(null);
                    userRepository.save(user);
                    log.info("Tài khoản '{}' đã tự động mở khóa sau 15 phút", user.getEmail());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Ghi nhận 1 lần đăng nhập thất bại do sai mật khẩu.
     */
    public void recordFailedAttempt(User user) {
        String key = user.getEmail() != null ? user.getEmail() : user.getUsername();
        ReentrantLock lock = getLockFor(key);
        lock.lock();
        try {
            int currentAttempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(currentAttempts);

            if (currentAttempts >= MAX_ATTEMPTS) {
                user.setLockoutExpiration(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                userRepository.save(user);
                log.warn("Tài khoản '{}' bị khóa 15 phút do nhập sai mật khẩu {} lần", user.getEmail(), currentAttempts);
                throw new AccountLockedException("Bạn đã nhập sai mật khẩu 5 lần. Tài khoản tạm thời bị khóa trong 15 phút!");
            } else {
                userRepository.save(user);
                int remaining = MAX_ATTEMPTS - currentAttempts;
                log.info("Tài khoản '{}' nhập sai mật khẩu lần {}. Còn lại {} lần thử", user.getEmail(), currentAttempts, remaining);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Đăng nhập thành công -> Reset bộ đếm và trạng thái khóa.
     */
    public void recordSuccess(User user) {
        String key = user.getEmail() != null ? user.getEmail() : user.getUsername();
        ReentrantLock lock = getLockFor(key);
        lock.lock();
        try {
            if (user.getFailedAttempts() > 0 || user.getLockoutExpiration() != null) {
                user.setFailedAttempts(0);
                user.setLockoutExpiration(null);
                userRepository.save(user);
            }
        } finally {
            lock.unlock();
        }
    }
}
