package com.karasuno.backend.service.privacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module 2 — Local Data Privacy Pre-Screening Shield.
 *
 * <p>Runs entirely in-memory before any payload leaves the server.
 * Detects and replaces Personally Identifiable Information (PII) with
 * clearly labelled redaction tokens. Covers:
 * <ul>
 *   <li>Email addresses</li>
 *   <li>Indian mobile and international phone numbers</li>
 *   <li>Financial amounts (₹, Rs, $, USD)</li>
 *   <li>Aadhaar numbers (12-digit Indian national ID)</li>
 *   <li>PAN card numbers (Indian tax ID format)</li>
 *   <li>Credit / debit card numbers (16-digit groups)</li>
 *   <li>Internal project tokens ([INTERNAL:...], PROJ-*, CONF-*)</li>
 * </ul>
 */
@Service
public class DataPrivacyShieldService {

    private static final Logger log = LoggerFactory.getLogger(DataPrivacyShieldService.class);

    // ── Detection patterns (applied in priority order) ────────────────────────

    private static final Pattern EMAIL =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern PAN =
            Pattern.compile("\\b[A-Z]{5}\\d{4}[A-Z]\\b");

    private static final Pattern AADHAAR =
            Pattern.compile("\\b\\d{4}[\\s\\-]?\\d{4}[\\s\\-]?\\d{4}\\b");

    private static final Pattern CREDIT_CARD =
            Pattern.compile("\\b(?:\\d{4}[\\s\\-]?){3}\\d{4}\\b");

    private static final Pattern FINANCIAL_AMOUNT =
            Pattern.compile(
                "(?:₹|Rs\\.?|INR|\\$|USD)\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?|" +
                "\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?\\s?(?:₹|Rs\\.?|INR|USD)",
                Pattern.CASE_INSENSITIVE);

    private static final Pattern PHONE =
            Pattern.compile(
                "(?:(?:\\+91|0091|91)?[\\s\\-]?)?[6-9]\\d{9}|" +
                "\\+?[1-9]\\d{1,3}[\\s\\-]?\\(?\\d{1,4}\\)?[\\s\\-]?\\d{1,4}[\\s\\-]?\\d{1,9}",
                Pattern.CASE_INSENSITIVE);

    private static final Pattern INTERNAL_TOKEN =
            Pattern.compile(
                "\\[INTERNAL:[^\\]]+\\]|PROJ-[A-Z0-9]{4,12}|CONF-[A-Z0-9]{4,12}",
                Pattern.CASE_INSENSITIVE);

    /** Priority-ordered list of PII detection rules. */
    private static final List<PiiRule> PII_RULES = List.of(
            new PiiRule(EMAIL,          "[EMAIL_REDACTED]"),
            new PiiRule(PAN,            "[PAN_REDACTED]"),
            new PiiRule(AADHAAR,        "[AADHAAR_REDACTED]"),
            new PiiRule(CREDIT_CARD,    "[CARD_REDACTED]"),
            new PiiRule(FINANCIAL_AMOUNT, "[AMOUNT_REDACTED]"),
            new PiiRule(PHONE,          "[PHONE_REDACTED]"),
            new PiiRule(INTERNAL_TOKEN, "[INTERNAL_TOKEN_REDACTED]")
    );

    /**
     * Scans and masks all PII in the input text.
     *
     * @param text the original draft text
     * @return MaskingResult with masked text and detection metadata
     */
    public MaskingResult maskPii(String text) {
        if (text == null || text.isBlank()) {
            return new MaskingResult(text, false, List.of());
        }

        String masked = text;
        List<String> detectedTypes = new ArrayList<>();

        for (PiiRule rule : PII_RULES) {
            Matcher matcher = rule.pattern().matcher(masked);
            if (matcher.find()) {
                masked = rule.pattern().matcher(masked).replaceAll(rule.mask());
                detectedTypes.add(rule.mask());
                log.info("PII masked: type={}", rule.mask());
            }
        }

        boolean piiFound = !detectedTypes.isEmpty();
        if (piiFound) {
            log.warn("Privacy Shield: {} PII type(s) detected and redacted", detectedTypes.size());
        }

        return new MaskingResult(masked, piiFound, detectedTypes);
    }

    /** Result of a PII masking operation. */
    public record MaskingResult(
            String maskedText,
            boolean piiDetected,
            List<String> detectedPiiTypes
    ) {}

    /** Internal coupling of a detection regex pattern to its replacement mask token. */
    private record PiiRule(Pattern pattern, String mask) {}
}
