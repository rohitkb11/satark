package in.scamcheck.core;

import java.io.IOException;
import java.util.*;
import static in.scamcheck.core.CheckResult.*;

/** Experimental AI may request review, but cannot clear rules or make a strong accusation alone. */
public final class AssistedChecker {
    private final ScamChecker rules;
    private final LocalTextModel model;
    public AssistedChecker() throws IOException { this(EngineResources.BUNDLED); }
    public AssistedChecker(EngineResources resources) throws IOException {
        rules = new ScamChecker(resources);
        LocalTextModel candidate;
        try { candidate = new LocalTextModel(resources.open("message_model.tsv")); } catch (IOException e) { candidate = null; }
        model = candidate;
    }
    public boolean modelAvailable() { return model != null; }
    AssistedChecker(ScamChecker rules, LocalTextModel model) { this.rules = rules; this.model = model; }
    public CheckResult check(String input, boolean useModel, boolean partial) {
        CheckResult baseline = rules.check(input, partial);
        if (!useModel) return baseline;
        List<Finding> findings = new ArrayList<>(baseline.findings());
        boolean languageLimited = findings.stream().anyMatch(f -> f.code().equals("LANGUAGE_LIMIT"));
        String method = "rules-only: model unavailable";
        Level level = baseline.level();
        if (model == null) {
            findings.add(new Finding("MODEL_UNAVAILABLE", Kind.UNCERTAINTY, "The local model could not load. Only the message and address rules ran."));
            if (level == Level.NO_WARNING_SIGNS) level = Level.NEEDS_CHECKING;
        } else if (languageLimited) method = "rules-only: model skipped for unsupported language";
        else {
            method = "rules + experimental local model";
            String text = input.substring(0, Math.min(input.length(), ScamChecker.MAX_CHARACTERS));
            if (model.suggestsReview(text)) {
                findings.add(new Finding("MODEL_REVIEW", Kind.UNCERTAINTY,
                    "The experimental model recognised wording similar to its synthetic scam examples. It can misread genuine messages; verify independently."));
                if (level == Level.NO_WARNING_SIGNS) level = Level.NEEDS_CHECKING;
            }
        }
        List<String> actions = new ArrayList<>(baseline.actions());
        if (level != baseline.level()) {
            actions.clear(); actions.add("Check the request through an independently known app or contact before acting.");
        }
        return new CheckResult(level, findings, baseline.links(), actions,
            List.of(baseline.limitations().get(0), baseline.limitations().get(1),
                "The local model was trained on small synthetic English examples. It is experimental and is off by default because evaluation found false warnings."),
            "0.3.0", method);
    }
}
