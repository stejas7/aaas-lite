package com.tejas.aaas.identity;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
/** Exactly one identity key, one approved date and a ZIP; never log this object. */
public record Triplet(String identityType, String identityValue, String dateType, String dateValue, String zip) {
    public void validate() {
        if (!Set.of("last4Ssn", "employeeId").contains(value(identityType))
            || !Set.of("dob", "hireDate").contains(value(dateType))) throw invalid();
        if (identityValue == null || (identityType.equals("last4Ssn")
            ? !identityValue.matches("[0-9]{4}") : !identityValue.matches("[A-Za-z0-9_-]{1,64}"))) throw invalid();
        if (zip == null || !zip.matches("[0-9]{5}(?:-[0-9]{4})?")) throw invalid();
        try {
            LocalDate date = LocalDate.parse(value(dateValue));
            if (date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900, 1, 1))) throw invalid();
        } catch (DateTimeParseException e) { throw invalid(); }
    }
    private static String value(String s) { return s == null ? "" : s; }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("Enter an approved identity combination with valid values."); }
    @Override public String toString() { return "Triplet[REDACTED]"; }
}
