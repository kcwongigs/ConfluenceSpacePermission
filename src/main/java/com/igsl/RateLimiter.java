package com.igsl;

public class RateLimiter {
	private int limit;
	private double available;
	private long interval;
	private long lastTimeStamp;

	RateLimiter(int limit, long interval) {
        this.limit = limit;
        this.interval = interval;
        available = 0;
        lastTimeStamp = System.currentTimeMillis();
    }

    synchronized boolean canProceed() {
        long now = System.currentTimeMillis();
        // more token are released since last request
        available += (now - lastTimeStamp) * 1.0 / interval * limit; 
        if (available > limit) {
            available = limit;
        }
        lastTimeStamp = now;
        if (available < 1) {
            return false;
        } else {
            available--;
            return true;
        }
    }
}
