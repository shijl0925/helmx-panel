package com.helmx.tutorial.docker.utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DockerHostCleanupFilter extends OncePerRequestFilter {

    private final DockerClientUtil dockerClientUtil;

    public DockerHostCleanupFilter(DockerClientUtil dockerClientUtil) {
        this.dockerClientUtil = dockerClientUtil;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            dockerClientUtil.clearCurrentHost();
        }
    }
}
