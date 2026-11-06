package com.yunrui.edu.common.id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 单机面试版雪花ID：41位时间戳 + 10位节点 + 12位序列。
 */
@Component
public class SnowflakeIdGenerator implements IdGenerator {
    private static final long EPOCH = 1704067200000L;
    private static final long SEQUENCE_MASK = 0xFFFL;

    private final long nodeId;
    private long lastMillis = -1L;
    private long sequence;

    public SnowflakeIdGenerator(@Value("${app.node-id:1}") long nodeId) {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("nodeId必须位于0到1023之间");
        }
        this.nodeId = nodeId;
    }

    @Override
    public synchronized long nextId() {
        long current = System.currentTimeMillis();
        if (current < lastMillis) {
            throw new IllegalStateException("检测到系统时钟回拨，拒绝生成订单号");
        }
        if (current == lastMillis) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                current = waitNextMillis(lastMillis);
            }
        } else {
            sequence = 0;
        }
        lastMillis = current;
        return ((current - EPOCH) << 22) | (nodeId << 12) | sequence;
    }

    private long waitNextMillis(long previous) {
        long current;
        do {
            Thread.onSpinWait();
            current = System.currentTimeMillis();
        } while (current <= previous);
        return current;
    }
}
