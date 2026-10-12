package in.scamcheck.core;

import java.net.IDN;
import java.net.URI;
import java.util.*;
import java.util.regex.*;
import static in.scamcheck.core.CheckResult.*;

final class UrlChecks {
    static final int MAX_LINKS = 32;
    private static final Pattern URL = Pattern.compile(
        "(?i)(?:[a-z][a-z0-9+.-]*://[^\\s<>\"']*|(?:https?|javascript|data|intent|file|upi):[^\\s<>\"']*|www\\.[^\\s<>\"']+|" +
        "(?<![\\p{L}\\p{N}@./_-])(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]*[\\p{L}\\p{N}])?\\.)+" +
        "[\\p{L}]{2,63}(?::[0-9]+)?(?:[/?#][^\\s<>\"']*)?)");
    private static final Set<String> SHORTENERS = Set.of("bit.ly", "tinyurl.com", "t.co", "cutt.ly", "rb.gy", "shorturl.at", "is.gd", "ow.ly");
    record Extracted(List<String> urls, boolean overflow) {}
    static Extracted extract(String text) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Matcher match = URL.matcher(text);
        while (match.find()) {
            String value = match.group();
            while (value.endsWith(".") || value.endsWith(",") || value.endsWith(";") || value.endsWith("!") || value.endsWith("?"))
                value = value.substring(0, value.length() - 1);
            for (char close : new char[]{')', ']', '}'}) {
                char open = close == ')' ? '(' : close == ']' ? '[' : '{';
                while (value.endsWith(String.valueOf(close)) && count(value, close) > count(value, open))
                    value = value.substring(0, value.length() - 1);
            }
            urls.add(value);
            if (urls.size() > MAX_LINKS) return new Extracted(List.copyOf(new ArrayList<>(urls).subList(0, MAX_LINKS)), true);
        }
        return new Extracted(List.copyOf(urls), false);
    }
    private static long count(String text, char c) { return text.chars().filter(x -> x == c).count(); }
    static Link inspect(String raw, BrandRegistry registry, PublicSuffixes suffixes,
                        List<BrandRegistry.Brand> claims) {
        List<Finding> out = new ArrayList<>();
        String host = "", site = "";
        try {
            String value = raw.matches("(?i)^[a-z][a-z0-9+.-]*://.*") ||
                raw.matches("(?i)^(?:https?|javascript|data|intent|file|upi):.*") ? raw : "https://" + raw;
            URI uri = new URI(value);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
                return unknown("UNSUPPORTED_LINK", "This link type is outside the website checker's scope.");
            String authority = uri.getRawAuthority();
            if (authority == null || authority.isBlank() || authority.contains("%") || authority.contains("\\"))
                return unknown("INVALID_LINK", "The address could not be parsed reliably. No website was opened.");
            int at = authority.lastIndexOf('@');
            if (at >= 0) {
                out.add(new Finding("ADDRESS_USERINFO", Kind.RISK, "The address contains text before an @ sign. That text is not the destination website."));
                authority = authority.substring(at + 1);
            }
            if (authority.startsWith("[")) {
                if (uri.getHost() == null) return unknown("INVALID_LINK", "The numeric address could not be parsed reliably.");
                host = uri.getHost().toLowerCase(Locale.ROOT);
            } else {
                String[] parts = authority.split(":", -1);
                if (parts.length > 2 || (parts.length == 2 && (!parts[1].matches("[0-9]{1,5}") || Integer.parseInt(parts[1]) > 65535)))
                    return unknown("INVALID_LINK", "The address has an invalid port or host.");
                host = parts[0].replaceAll("\\.$", "");
                boolean international = !host.chars().allMatch(c -> c < 128) || host.toLowerCase(Locale.ROOT).contains("xn--");
                host = IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
                if (international) out.add(new Finding("INTERNATIONAL_ADDRESS", Kind.UNCERTAINTY,
                    "The website uses international characters. These can be legitimate or look like another name; check the displayed address."));
            }
            if (host.isBlank() || host.length() > 253) return unknown("INVALID_LINK", "The website address is missing or too long.");
            boolean numeric = host.startsWith("[") || host.matches("[0-9.]+") || host.matches("(?i)0x[0-9a-f]+");
            if (numeric) {
                out.add(new Finding("NUMERIC_ADDRESS", Kind.UNCERTAINTY, "This link uses a numeric destination rather than a recognised organisation website."));
                return new Link(host, host, "", false, out);
            }
            if (!host.contains(".")) return unknown("LOCAL_ADDRESS", "This is a local or incomplete address; it cannot identify a public organisation.");
            site = suffixes.registrableDomain(host);
            if (site.isEmpty()) return unknown("PUBLIC_SUFFIX_ONLY", "This address does not identify an individual website.");
            if (!suffixes.knownSuffix(host)) out.add(new Finding("UNKNOWN_SUFFIX", Kind.UNCERTAINTY,
                "The website ending is absent from the bundled address list."));
            if (SHORTENERS.contains(site)) out.add(new Finding("SHORTENED_LINK", Kind.UNCERTAINTY,
                "This is a shortened link. Its final destination cannot be checked offline."));
            if ("http".equalsIgnoreCase(uri.getScheme())) out.add(new Finding("UNENCRYPTED_LINK", Kind.RISK,
                "The link starts with HTTP. It does not request an encrypted connection; HTTPS alone would not prove safety either."));
            BrandRegistry.Brand known = registry.forHost(host);
            if (known != null) {
                out.add(new Finding("KNOWN_BRAND_ADDRESS", Kind.INFORMATION,
                    "The address matches a saved website entry for " + known.name() + ". This does not verify the sender or page contents."));
                if (!claims.isEmpty() && claims.stream().noneMatch(b -> b.id().equals(known.id())))
                    out.add(new Finding("CLAIM_ADDRESS_MISMATCH", Kind.RISK,
                        "The destination matches " + known.name() + ", but not the organisation named in the message."));
            }
            else {
                for (BrandRegistry.Brand b : registry.brands()) {
                    boolean embedded = b.roots().stream().anyMatch(root -> hostContainsRoot(rawHost(raw), root));
                    // Restrict edit-distance comparisons to sufficiently long brand names.
                    String first = site.split("\\.")[0];
                    boolean resemblance = b.id().length() >= 4 && (first.contains(b.id()) ||
                        first.startsWith(b.id().replace("i", "1")) || distance(first, b.id()) <= 1);
                    if (embedded || resemblance) {
                        out.add(new Finding("BRAND_LOOKALIKE", Kind.RISK,
                            "The address resembles " + b.name() + ", but its website address is " + site + ", outside that brand's saved entries."));
                        break;
                    }
                }
                if (!claims.isEmpty()) out.add(new Finding("CLAIM_ADDRESS_MISMATCH", Kind.RISK,
                    "The message mentions " + String.join(", ", claims.stream().map(BrandRegistry.Brand::name).toList()) +
                    ", but this destination is outside their saved website entries. Other legitimate addresses may be missing."));
                out.add(new Finding("UNRECOGNISED_ADDRESS", Kind.UNCERTAINTY,
                    "This address is not in the small saved organisation list. An unfamiliar website is not automatically fraudulent."));
            }
            return new Link(host, site, known == null ? "" : known.name(), known != null, out);
        } catch (Exception ex) {
            return unknown("INVALID_LINK", "The address could not be parsed reliably. No website was opened.");
        }
    }
    private static String rawHost(String raw) {
        try { return new URI(raw.contains("://") ? raw : "https://" + raw).getRawAuthority().toLowerCase(Locale.ROOT); }
        catch (Exception e) { return ""; }
    }
    private static boolean hostContainsRoot(String host, String root) {
        // Only before the @ destination or as a full prefix/subdomain, never a query/path match.
        return host.contains(root + ".") || host.contains(root + "@");
    }
    private static Link unknown(String code, String reason) {
        return new Link("", "", "", false, List.of(new Finding(code, Kind.UNCERTAINTY, reason)));
    }
    private static int distance(String a, String b) {
        if (Math.abs(a.length() - b.length()) > 1) return 2;
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] row = new int[b.length() + 1]; row[0] = i;
            for (int j = 1; j <= b.length(); j++) row[j] = Math.min(Math.min(row[j - 1] + 1, prev[j] + 1),
                prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            prev = row;
        }
        return prev[b.length()];
    }
}
