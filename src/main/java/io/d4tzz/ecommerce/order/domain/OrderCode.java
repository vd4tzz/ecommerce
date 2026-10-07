package io.d4tzz.ecommerce.order.domain;

import io.d4tzz.ecommerce.common.exception.InvalidArgumentException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record OrderCode(String value) {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    public OrderCode {
        if (value.isBlank()) {
            throw new InvalidArgumentException("Order code must not be blank");
        }
        value = value.trim().toUpperCase();
    }

    public static OrderCode of(String value) {
        return new OrderCode(value);
    }

    public static OrderCode generate() {
        String datePart = LocalDate.now(ZONE).format(DateTimeFormatter.ofPattern("yyMMdd"));
        StringBuilder randomPart = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            randomPart.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new OrderCode("ORD" + datePart + randomPart);
    }

    @Override
    public String toString() {
        return value;
    }
}
