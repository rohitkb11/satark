package in.scamcheck.cli;

import in.scamcheck.core.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class Main {
    public static void main(String[] args) throws Exception {
        ScamChecker checker = new ScamChecker();
        if (args.length == 1 && args[0].equals("--demo")) {
            String[] examples = {
                "HDFC Bank: Your account will be blocked. Update KYC immediately at https://hdfc.bank.in.fake-site.com/verify",
                "Your OTP is 123456. Do not share your OTP with anyone.",
                "Check this link: https://bit.ly/example",
                "Amazon: Your order has shipped. View it at https://www.amazon.in/orders",
                "Urgent! Pay a processing fee to claim your refund at https://refund.fake-site.com"
            };
            for (int i = 0; i < examples.length; i++) { System.out.println("\nDemo " + (i + 1)); print(checker.check(examples[i])); }
            return;
        }
        System.out.println("ScamCheck offline baseline. Paste a message, then press Enter on an empty line. Nothing is saved.");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        StringBuilder input = new StringBuilder(); String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            input.append(line).append('\n');
            if (input.length() > ScamChecker.MAX_CHARACTERS) break;
        }
        try { print(checker.check(input.toString())); }
        catch (IllegalArgumentException e) { System.err.println(e.getMessage()); System.exit(2); }
    }
    private static void print(CheckResult result) {
        System.out.println(result.level().label + " | " + result.method());
        for (CheckResult.Link link : result.links()) {
            System.out.println("Website address: " + (link.siteAddress().isEmpty() ? "unknown" : link.siteAddress()));
        }
        for (CheckResult.Finding f : result.findings()) System.out.println("- [" + f.kind() + "] " + f.explanation());
        for (String action : result.actions()) System.out.println("Next: " + action);
        System.out.println("Limit: " + result.limitations().get(0));
    }
}
