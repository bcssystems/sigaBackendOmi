package com.bcsystems.siga.controller;
import org.springframework.security.core.context.SecurityContextHolder;
final class SecurityContextHolderHelper {
    private SecurityContextHolderHelper() {}
    static String username(){return SecurityContextHolder.getContext().getAuthentication().getName();}
}
