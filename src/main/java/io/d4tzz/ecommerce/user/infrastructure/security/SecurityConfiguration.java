package io.d4tzz.ecommerce.user.infrastructure.security;

import io.d4tzz.ecommerce.user.domain.auth.OtpGenerator;
import io.d4tzz.ecommerce.user.domain.auth.OtpRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {

    @Bean
    public OtpGenerator otpGenerator(OtpRepository otpRepository) {
        return new OtpGenerator(otpRepository);
    }
}
