package in.scamcheck.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Small real trained model; pure Java inference with no cloud/runtime dependencies. */
public final class LocalTextModel {
    private static final Pattern WORD = Pattern.compile("[a-z]+");
    private final double[] weights;
    private final double bias, threshold;
    public LocalTextModel() throws IOException { this(LocalTextModel.class.getResourceAsStream("/message_model.tsv")); }
    public LocalTextModel(InputStream stream) throws IOException {
        if (stream == null) throw new IOException("Local model missing");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            if (!"scamcheck-linear-v1".equals(reader.readLine())) throw new IOException("Unsupported model format");
            int dimension = Integer.parseInt(reader.readLine());
            if (dimension != 4096) throw new IOException("Unsupported feature dimensions");
            bias = Double.parseDouble(reader.readLine()); threshold = Double.parseDouble(reader.readLine());
            if (!Double.isFinite(bias) || !Double.isFinite(threshold) || threshold <= 0 || threshold >= 1) throw new IOException("Invalid model parameters");
            weights = new double[dimension];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = Double.parseDouble(reader.readLine());
                if (!Double.isFinite(weights[i])) throw new IOException("Invalid model weight");
            }
            if (reader.readLine() != null) throw new IOException("Unexpected model data");
        } catch (NumberFormatException | NullPointerException e) { throw new IOException("Invalid model data", e); }
    }
    public double score(String input) {
        String text = MessageChecks.normalize(input).replaceAll("https?://\\S+|www\\.\\S+", " ");
        List<String> words = new ArrayList<>();
        Matcher matcher = WORD.matcher(text);
        while (matcher.find() && words.size() < 256) words.add(matcher.group());
        Set<Integer> indices = new TreeSet<>();
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i); add(indices, "w:" + word);
            if (i > 0) add(indices, "b:" + words.get(i - 1) + ":" + word);
            String token = "^" + word + "$";
            for (int length = 3; length <= 5; length++) for (int start = 0; start + length <= token.length(); start++)
                add(indices, "c:" + token.substring(start, start + length));
        }
        double value = bias;
        for (int index : indices) value += weights[index];
        return 1.0 / (1.0 + Math.exp(-Math.max(-35, Math.min(35, value))));
    }
    public boolean suggestsReview(String input) { return score(input) >= threshold; }
    public double threshold() { return threshold; }
    public String version() { return "synthetic-linear-v1"; }
    private void add(Set<Integer> indices, String feature) { indices.add(Math.floorMod(feature.hashCode(), weights.length)); }
}
