package com.helmx.tutorial.docker.utils;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DockerHostCleanupFilterTest {

    @Mock
    private DockerClientUtil dockerClientUtil;

    @Test
    void doFilter_clearsCurrentHostAfterSuccessfulRequest() throws ServletException, IOException {
        DockerHostCleanupFilter filter = new DockerHostCleanupFilter(dockerClientUtil);

        filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> { }
        );

        verify(dockerClientUtil).clearCurrentHost();
    }

    @Test
    void doFilter_clearsCurrentHostWhenChainThrows() throws ServletException, IOException {
        DockerHostCleanupFilter filter = new DockerHostCleanupFilter(dockerClientUtil);

        assertThrows(ServletException.class, () -> filter.doFilter(
                new MockHttpServletRequest(),
                new MockHttpServletResponse(),
                (request, response) -> {
                    throw new ServletException("boom");
                }
        ));

        verify(dockerClientUtil).clearCurrentHost();
    }
}
