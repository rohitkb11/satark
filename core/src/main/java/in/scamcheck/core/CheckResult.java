package in.scamcheck.core;

import java.util.List;

/** No original message or complete URLs are retained in this result. */
public record CheckResult(Level level, List<Finding> findings, List<Link> links,
                          List<String> actions, List<String> limitations,
                          String engineVersion, String method) {
    public CheckResult {
        findings = List.copyOf(findings);
        links = List.copyOf(links);
        actions = List.copyOf(actions);
        limitations = List.copyOf(limitations);
    }
    public enum Level {
        STRONG_WARNING("Strong warning"), NEEDS_CHECKING("Needs checking"),
        NO_WARNING_SIGNS("No warning signs found");
        public final String label;
        Level(String label) { this.label = label; }
    }
    public enum Kind { RISK, UNCERTAINTY, INFORMATION }
    public record Finding(String code, Kind kind, String explanation) {}
    /** Domain recognition is an identity clue, never a safety verdict. */
    public record Link(String host, String siteAddress, String matchedBrand,
                       boolean knownAddress, List<Finding> findings) {
        public Link { findings = List.copyOf(findings); }
    }
}
