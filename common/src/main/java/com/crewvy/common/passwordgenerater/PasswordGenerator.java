package com.crewvy.common.passwordgenerater;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PasswordGenerator {
    private static final String UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGIT_CHARS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*+-<>/";
    private static final String ALL_CHARS = UPPERCASE_CHARS + LOWERCASE_CHARS + DIGIT_CHARS + SPECIAL_CHARS;
    private static final int PASSWORD_LENGTH = 9;

    public static String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);

        password.append(UPPERCASE_CHARS.charAt(random.nextInt(UPPERCASE_CHARS.length())));
        password.append(LOWERCASE_CHARS.charAt(random.nextInt(LOWERCASE_CHARS.length())));
        password.append(DIGIT_CHARS.charAt(random.nextInt(DIGIT_CHARS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        IntStream.range(0, PASSWORD_LENGTH - 4)
                .forEach(i -> password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length()))));

        List<Character> passwordChars = password.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(passwordChars, random);

        return passwordChars.stream()
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }
}
