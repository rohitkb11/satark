package in.scamcheck.core;

import static in.scamcheck.core.CheckResult.*;

/** Regression tests of important boundaries, not an accuracy benchmark. */
public final class CoreTests {
    private static int passed;
    private static ScamChecker checker;
    public static void main(String[] args) throws Exception {
        checker = new ScamChecker();
        level("KYC impersonation", "HDFC Bank: account will be blocked. Update KYC immediately https://hdfc.bank.in.fake-site.com", Level.STRONG_WARNING);
        level("OTP disclosure pressure", "Urgent! Share your OTP with our agent immediately", Level.STRONG_WARNING);
        level("Ordinary OTP alert", "Your OTP is 123456. Do not share your OTP with anyone.", Level.NO_WARNING_SIGNS);
        level("Bank never requests OTP", "ICICI Bank will never ask you to share your OTP", Level.NO_WARNING_SIGNS);
        level("Never-share notice", "Never share your password or PIN", Level.NO_WARNING_SIGNS);
        level("Normal shipping", "Amazon: your order shipped. View at https://www.amazon.in/orders", Level.NO_WARNING_SIGNS);
        level("Official banking address", "HDFC Bank: visit https://www.hdfc.bank.in", Level.NO_WARNING_SIGNS);
        level("Normal informal text", "See you tomorrow at five", Level.NO_WARNING_SIGNS);
        level("Payment prevention advice", "Never pay to claim a refund", Level.NO_WARNING_SIGNS);
        level("APK prevention advice", "Do not install AnyDesk APK", Level.NO_WARNING_SIGNS);
        level("Urgency alone", "Urgent: meeting moved to noon", Level.NEEDS_CHECKING);
        level("Unknown is not fraud", "Read https://news.example.org/story", Level.NEEDS_CHECKING);
        level("Short link unknown", "https://bit.ly/sample", Level.NEEDS_CHECKING);
        level("Numeric address", "https://192.0.2.1/login", Level.NEEDS_CHECKING);
        level("IPv6", "https://[2001:db8::1]/login", Level.NEEDS_CHECKING);
        level("Malformed port", "https://hdfc.bank.in:99999", Level.NEEDS_CHECKING);
        level("Missing host", "https://", Level.NEEDS_CHECKING);
        level("Local address", "https://localhost/login", Level.NEEDS_CHECKING);
        level("Unsupported scheme", "ftp://files.example.com", Level.NEEDS_CHECKING);
        level("Javascript link unsupported", "javascript:alert(1)", Level.NEEDS_CHECKING);
        level("Payment intent unsupported", "upi://pay?pa=example@bank", Level.NEEDS_CHECKING);
        level("Broken HTTPS address", "https:/", Level.NEEDS_CHECKING);
        level("HTTP on known website", "http://amazon.in", Level.NEEDS_CHECKING);
        level("Pay to claim refund", "Urgent! Pay a fee to claim your refund", Level.STRONG_WARNING);
        level("Remote app request", "Urgent: install AnyDesk APK", Level.STRONG_WARNING);
        level("Known link does not cancel text", "Urgent! Share your password. https://amazon.in", Level.STRONG_WARNING);
        level("Wrong known organisation", "HDFC Bank: Update KYC at https://amazon.in", Level.STRONG_WARNING);
        level("Link in bank news not strong", "News about HDFC Bank https://news.example.org/story", Level.NEEDS_CHECKING);
        level("Non-Latin uncertainty", "अपना खाता जांचें", Level.NEEDS_CHECKING);
        level("Hidden formatting", "Hello\u200b world", Level.NEEDS_CHECKING);
        assertTrue(checker.check("Hello", true).level() == Level.NEEDS_CHECKING, "Partial notification cannot clear");
        assertTrue(checker.check("a".repeat(12001)).level() == Level.NEEDS_CHECKING, "Input limit is explicit");
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 34; i++) many.append("https://example" + i + ".com ");
        CheckResult limited = checker.check(many.toString());
        assertTrue(limited.links().size() == 32 && code(limited, "LINK_LIMIT"), "Link limit is explicit");
        link("Subdomain trick", "https://hdfc.bank.in.evil.com", "evil.com", false);
        link("Brand suffix trick", "https://amazon.in.evil.co.uk", "evil.co.uk", false);
        link("Official subdomain", "https://www.amazon.in/", "amazon.in", true);
        link("Case and trailing dot", "https://WWW.AMAZON.IN./", "amazon.in", true);
        link("Old HDFC redirect root", "https://www.hdfcbank.com", "hdfcbank.com", true);
        link("Current ICICI root", "https://www.icici.bank.in", "icici.bank.in", true);
        link("Bare website extraction", "amazon.in/orders", "amazon.in", true);
        link("Punctuation", "Look here (https://amazon.in/orders).", "amazon.in", true);
        link("Private hosting boundary", "https://amazon.github.io", "amazon.github.io", false);
        link("PSL wildcard", "https://a.b.ck", "a.b.ck", false);
        link("PSL exception", "https://www.ck", "www.ck", false);
        link("International domain", "https://bücher.de", "xn--bcher-kva.de", false);
        assertTrue(code(checker.check("https://bücher.de"), "INTERNATIONAL_ADDRESS"), "Unicode address uncertainty");
        assertTrue(code(checker.check("https://hdfc.bank.in@evil.com"), "ADDRESS_USERINFO"), "Userinfo deception");
        assertTrue(!checker.check("https://evil.com/path/amazon.in").links().get(0).knownAddress(), "Path cannot grant brand recognition");
        assertTrue(!checker.check("https://evil.com?next=https://amazon.in").links().get(0).knownAddress(), "Query cannot grant recognition");
        assertTrue(checker.check("Contact support@example.org").links().isEmpty(), "Email is not a website input");
        assertTrue(checker.check("https://amazon.in https://amazon.in").links().size() == 1, "Repeated links deduplicate");
        assertTrue(checker.check("https://amazon.in/account?token=SECRET_TOKEN").toString().indexOf("SECRET_TOKEN") < 0, "Result omits URL query secrets");
        assertTrue(!checker.check("plain text SECRET_MESSAGE").toString().contains("SECRET_MESSAGE"), "Result omits raw message");
        assertTrue(checker.check("test").method().equals("rules-only"), "Method disclosure");
        boolean emptyRejected = false;
        try { checker.check(" "); } catch (IllegalArgumentException e) { emptyRejected = true; }
        assertTrue(emptyRejected, "Empty input rejected");
        assertTrue(code(checker.check("Approve this UPI collect request to receive your refund"),"UPI_COLLECT_TO_RECEIVE"),"Collect to receive workflow");
        assertTrue(code(checker.check("Scan this QR code to get cashback"),"QR_TO_RECEIVE"),"QR to receive workflow");
        assertTrue(!code(checker.check("Never approve a UPI collect request to receive a refund"),"UPI_COLLECT_TO_RECEIVE"),"Collect prevention advice");
        assertTrue(!code(checker.check("Do not scan a QR code to receive money"),"QR_TO_RECEIVE"),"QR prevention advice");
        level("Legitimate payment approval","Approve the payment request to pay your electricity bill",Level.NO_WARNING_SIGNS);
        System.out.println("PASS: " + passed + " offline core regression checks. These are not detection-accuracy results.");
    }
    private static boolean code(CheckResult result, String code) { return result.findings().stream().anyMatch(f -> f.code().equals(code)); }
    private static void level(String label, String input, Level expected) {
        CheckResult result = checker.check(input);
        assertTrue(result.level() == expected, label + " expected " + expected + ", received " + result.level());
    }
    private static void link(String label, String input, String expected, boolean known) {
        CheckResult result = checker.check(input);
        assertTrue(result.links().size() == 1 && result.links().get(0).siteAddress().equals(expected) &&
            result.links().get(0).knownAddress() == known, label + " incorrect destination or recognition: " + result.links());
    }
    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        passed++;
    }
}
