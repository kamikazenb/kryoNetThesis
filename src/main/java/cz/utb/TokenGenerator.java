package cz.utb;

import java.security.SecureRandom;
import java.util.Random;

public class TokenGenerator {

    private final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890@";

    public String generateRandom(int length) {
        Random random = new SecureRandom();
        if (length <= 0) {
            throw new IllegalArgumentException("String length must be a positive integer");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }
}