package com.yunrui.edu.common.lock;

import java.time.Duration;
import java.util.Optional;

public interface DistributedLockManager {
    Optional<LockHandle> tryLock(String key, Duration leaseTime);

    interface LockHandle extends AutoCloseable {
        @Override
        void close();
    }
}
