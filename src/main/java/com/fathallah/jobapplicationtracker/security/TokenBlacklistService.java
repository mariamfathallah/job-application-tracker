package com.fathallah.jobapplicationtracker.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedJtis = ConcurrentHashMap.newKeySet();

    public void invalidate(String jti) {
        blacklistedJtis.add(jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklistedJtis.contains(jti);
    }
}
