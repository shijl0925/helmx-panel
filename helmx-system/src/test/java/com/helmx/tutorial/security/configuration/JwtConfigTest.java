package com.helmx.tutorial.security.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertSame;

class JwtConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class, JwtDecoderConsumerConfig.class)
            .withPropertyValues(
                    "app.jwt.private-key-path=classpath:private_key.pem",
                    "app.jwt.public-key-path=classpath:public_key.pem"
            );

    @Test
    void contextInjectsPrimaryJwtDecoderWhenMultipleJwtDecodersExist() {
        contextRunner.run(context -> {
            JwtDecoder primaryDecoder = context.getBean("jwtDecoder", JwtDecoder.class);
            JwtDecoder refreshDecoder = context.getBean("refreshJwtDecoder", JwtDecoder.class);
            JwtDecoderConsumer consumer = context.getBean(JwtDecoderConsumer.class);

            assertSame(primaryDecoder, consumer.jwtDecoder());
            org.junit.jupiter.api.Assertions.assertNotSame(refreshDecoder, consumer.jwtDecoder());
        });
    }

    record JwtDecoderConsumer(JwtDecoder jwtDecoder) {
    }

    @Configuration
    static class JwtDecoderConsumerConfig {
        @Bean
        JwtDecoderConsumer jwtDecoderConsumer(JwtDecoder jwtDecoder) {
            return new JwtDecoderConsumer(jwtDecoder);
        }
    }
}
