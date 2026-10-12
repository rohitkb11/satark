package in.scamcheck.core;

import java.io.*;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Offline PSL matcher, including private suffixes, wildcard and exception rules. */
public final class PublicSuffixes {
    private final Set<String> exact = new HashSet<>(), wildcard = new HashSet<>(), exception = new HashSet<>();
    public PublicSuffixes() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/public_suffix_list.dat")) {
            if (stream == null) throw new IOException("Bundled public suffix list is missing");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("//")) continue;
                    if (line.startsWith("!")) exception.add(ascii(line.substring(1)));
                    else if (line.startsWith("*.")) wildcard.add(ascii(line.substring(2)));
                    else exact.add(ascii(line));
                }
            }
        }
        if (exact.size() < 1000) throw new IOException("Public suffix list is incomplete");
    }
    private static String ascii(String value) {
        return IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES | IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
    }
    public String registrableDomain(String host) {
        String[] labels = host.split("\\.");
        int suffixLabels = 1;
        for (int i = 0; i < labels.length; i++) {
            String tail = String.join(".", Arrays.copyOfRange(labels, i, labels.length));
            if (exception.contains(tail)) { suffixLabels = labels.length - i - 1; break; }
            if (exact.contains(tail)) suffixLabels = Math.max(suffixLabels, labels.length - i);
            if (i > 0 && wildcard.contains(tail)) suffixLabels = Math.max(suffixLabels, labels.length - i + 1);
        }
        if (labels.length <= suffixLabels) return "";
        return String.join(".", Arrays.copyOfRange(labels, labels.length - suffixLabels - 1, labels.length));
    }
    public boolean knownSuffix(String host) {
        String[] labels = host.split("\\.");
        for (int i = 0; i < labels.length; i++) {
            String tail = String.join(".", Arrays.copyOfRange(labels, i, labels.length));
            if (exact.contains(tail) || exception.contains(tail) || (i > 0 && wildcard.contains(tail))) return true;
        }
        return false;
    }
}
