package in.scamcheck.core;

import java.io.IOException;
import java.util.*;
import static in.scamcheck.core.CheckResult.*;

/** Stateless, deterministic offline baseline. No network, storage or model claims. */
public final class ScamChecker {
    public static final String VERSION = "0.1.0-rules";
    public static final int MAX_CHARACTERS = 12000;
    private final BrandRegistry brands;
    private final PublicSuffixes suffixes;
    public ScamChecker() throws IOException { this(EngineResources.BUNDLED); }
    public ScamChecker(EngineResources resources) throws IOException { brands = new BrandRegistry(resources.open("brands.tsv")); suffixes = new PublicSuffixes(); }
    public CheckResult check(String input) { return check(input, false); }
    public CheckResult check(String input, boolean partialInput) {
        if (input == null || input.isBlank()) throw new IllegalArgumentException("Paste or share a message or link to check.");
        List<Finding> findings = new ArrayList<>();
        boolean truncated = input.length() > MAX_CHARACTERS;
        String original = truncated ? input.substring(0, MAX_CHARACTERS) : input;
        if (truncated || partialInput) findings.add(new Finding("PARTIAL_INPUT", Kind.UNCERTAINTY,
            "Only part of the content was checked. Share the full message for a more complete check."));
        boolean otherScript = original.codePoints().anyMatch(cp -> Character.isLetter(cp) &&
            Character.UnicodeScript.of(cp) != Character.UnicodeScript.LATIN);
        if (otherScript) findings.add(new Finding("LANGUAGE_LIMIT", Kind.UNCERTAINTY,
            "This baseline mainly checks English text. Other scripts and languages have incomplete coverage."));
        if (original.matches("(?s).*\\p{Cf}.*")) findings.add(new Finding("HIDDEN_CHARACTERS", Kind.UNCERTAINTY,
            "The text contains hidden formatting characters. They were removed for the check."));
        String text = MessageChecks.normalize(original);
        MessageChecks.Signals signals = MessageChecks.inspect(text);
        findings.addAll(signals.findings());
        // Brand claims exclude URL text: a copied address is not a sender claim.
        UrlChecks.Extracted extracted = UrlChecks.extract(text);
        String prose = text;
        for (String url : extracted.urls()) prose = prose.replace(url, " ");
        List<BrandRegistry.Brand> claims = brands.claimsIn(prose);
        List<Link> links = new ArrayList<>();
        for (String url : extracted.urls()) links.add(UrlChecks.inspect(url, brands, suffixes, claims));
        if (extracted.overflow()) findings.add(new Finding("LINK_LIMIT", Kind.UNCERTAINTY,
            "Only the first 32 different links were checked. Check the remaining links separately."));
        for (Link link : links) findings.addAll(link.findings());
        boolean deceptiveAddress = has(findings, "BRAND_LOOKALIKE") || has(findings, "ADDRESS_USERINFO");
        boolean mismatch = has(findings, "CLAIM_ADDRESS_MISMATCH");
        // Message and address evidence must agree. Correlated URL clues are not counted as independent votes.
        boolean strong = (signals.secretRequest() && signals.urgent()) ||
            ((deceptiveAddress || mismatch) && (signals.secretRequest() || signals.sensitiveAction() ||
                (signals.urgent() && signals.financialAction()))) ||
            (has(findings, "PAY_TO_RECEIVE") && (signals.urgent() || deceptiveAddress)) ||
            (has(findings, "REMOTE_APP_REQUEST") && (signals.urgent() || mismatch));
        boolean concern = findings.stream().anyMatch(f -> f.kind() != Kind.INFORMATION);
        Level level = strong ? Level.STRONG_WARNING : concern ? Level.NEEDS_CHECKING : Level.NO_WARNING_SIGNS;
        List<String> actions = new ArrayList<>();
        if (level != Level.NO_WARNING_SIGNS) {
            actions.add("Verify the request independently through the organisation's official app or a previously known contact.");
            actions.add("Do not disclose OTPs, passwords, PINs, or card security codes to a caller or message sender.");
        } else actions.add("Continue cautiously. No warning signs found is not a guarantee of safety.");
        LinkedHashMap<String, Finding> unique = new LinkedHashMap<>();
        for (Finding f : findings) unique.putIfAbsent(f.code() + ":" + f.explanation(), f);
        return new CheckResult(level, new ArrayList<>(unique.values()), links, actions,
            List.of("Offline checks cannot verify the sender, inspect a live page, follow redirects, or establish that a website is safe.",
                "The saved organisation list is small and may omit legitimate websites.",
                "This check uses explicit message and address rules. The local model is a separate optional check."),
            VERSION, "rules-only");
    }
    private static boolean has(List<Finding> findings, String code) { return findings.stream().anyMatch(f -> f.code().equals(code)); }
}
