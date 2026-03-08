package com.helmx.tutorial.docker.utils;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.helmx.tutorial.docker.mapper.DockerEnvMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DockerHostValidatorTest {

    @Mock
    private DockerEnvMapper dockerEnvMapper;

    private DockerHostValidator dockerHostValidator;

    @BeforeEach
    void setUp() {
        dockerHostValidator = new DockerHostValidator(dockerEnvMapper);
    }

    @Test
    void validateHostAllowlist_blankHost_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> dockerHostValidator.validateHostAllowlist(" "));
    }

    @Test
    void validateHostAllowlist_unknownHost_throwsIllegalArgumentException() {
        when(dockerEnvMapper.exists(any(Wrapper.class))).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> dockerHostValidator.validateHostAllowlist("tcp://unknown:2375"));
    }

    @Test
    void validateHostAllowlist_allowedHost_doesNotThrow() {
        when(dockerEnvMapper.exists(any(Wrapper.class))).thenReturn(true);

        assertDoesNotThrow(() -> dockerHostValidator.validateHostAllowlist("tcp://docker:2375"));
    }
}
