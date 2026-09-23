package io.d4tzz.ecommerce.infrastructure.security;

import io.d4tzz.ecommerce.domain.security.OtpGenerator;
import io.d4tzz.ecommerce.domain.security.OtpRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {

    @Bean
    public OtpGenerator otpGenerator(OtpRepository otpRepository) {
        return new OtpGenerator(otpRepository);
    }
}
