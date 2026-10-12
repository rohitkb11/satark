package in.scamcheck.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public final class BrandRegistry {
    public record Brand(String id, String name, List<String> aliases, List<String> roots,
                        String source, String checkedDate) {}
    private final List<Brand> brands;
    public BrandRegistry() throws IOException { this(EngineResources.BUNDLED.open("brands.tsv")); }
    public BrandRegistry(InputStream input) throws IOException {
        List<Brand> loaded = new ArrayList<>();
        try (InputStream stream = input) {
            if (stream == null) throw new IOException("Bundled brand registry is missing");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) continue;
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 6) throw new IOException("Invalid brand registry entry");
                    loaded.add(new Brand(fields[0], fields[1], List.of(fields[2].split(",")),
                        List.of(fields[3].split(",")), fields[4], fields[5]));
                }
            }
        }
        brands = List.copyOf(loaded);
    }
    public List<Brand> brands() { return brands; }
    public List<Brand> claimsIn(String text) {
        List<Brand> result = new ArrayList<>();
        for (Brand b : brands) {
            for (String alias : b.aliases()) {
                if (Pattern.compile("(?i)(?<![\\p{L}\\p{N}])" + Pattern.quote(alias) +
                        "(?![\\p{L}\\p{N}])").matcher(text).find()) {
                    result.add(b); break;
                }
            }
        }
        return result;
    }
    public Brand forHost(String host) {
        for (Brand b : brands) for (String root : b.roots()) {
            if (host.equals(root) || host.endsWith("." + root)) return b;
        }
        return null;
    }
}
