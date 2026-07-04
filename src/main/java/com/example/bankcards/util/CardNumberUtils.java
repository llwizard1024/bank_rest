package com.example.bankcards.util;

public final class CardNumberUtils {

    private CardNumberUtils() {
    }

    public static String normalize(String cardNumber) {
        return cardNumber.replaceAll("\\D", "");
    }

    public static String mask(String cardNumber) {
        String digits = normalize(cardNumber);
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Card number is too short");
        }
        String lastFour = lastFour(digits);
        return "**** **** **** " + lastFour;
    }

    public static String lastFour(String cardNumber) {
        String digits = normalize(cardNumber);
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Card number is too short");
        }
        return digits.substring(digits.length() - 4);
    }

    public static boolean isValid(String cardNumber) {
        String digits = normalize(cardNumber);
        if (digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        return passesLuhnCheck(digits);
    }

    private static boolean passesLuhnCheck(String digits) {
        int sum = 0;
        boolean doubleDigit = false;

        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(digits.charAt(i));
            if (doubleDigit) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }
}
