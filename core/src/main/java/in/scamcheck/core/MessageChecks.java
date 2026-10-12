package in.scamcheck.core;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import static in.scamcheck.core.CheckResult.*;

final class MessageChecks {
    record Signals(boolean urgent, boolean secretRequest, boolean financialAction,
                   boolean sensitiveAction, List<Finding> findings) {}
    private static boolean has(String value, String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(value).find();
    }
    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC)
            .replaceAll("[\\p{Cf}]", "").toLowerCase(Locale.ROOT);
    }
    static Signals inspect(String text) {
        List<Finding> out = new ArrayList<>();
        // Sentence-scoped: a legitimate 'do not share OTP' notice is not a request.
        boolean secret = false, urgent = false, action = false, sensitive = false, payToReceive = false, remoteApp = false, collectToReceive = false, qrToReceive = false;
        for (String sentence : text.split("[.!?\\n]+")) {
            boolean negated = has(sentence, "\\b(?:do not|don't|never|must not|will not|won't)\\b.{0,45}\\b(?:share|send|tell|provide|enter|disclose|reveal)\\b");
            boolean request = has(sentence, "\\b(?:share|send|tell|provide|enter|disclose|reveal)\\b.{0,55}\\b(?:otp|one.time password|password|pin|cvv|verification code)\\b") ||
                has(sentence, "\\b(?:otp|password|pin|cvv)\\b.{0,35}\\b(?:required|needed|bhejo|batao|share karo)\\b");
            if (request && !negated) secret = true;
            if (has(sentence, "\\b(?:urgent|immediately|last chance|within \\d+ (?:minute|hour)s?|account (?:will be |is )?(?:blocked|closed|suspended)|abhi|turant)\\b")) urgent = true;
            if (has(sentence, "\\b(?:pay|payment|transfer|send money|processing fee|release fee|refund|reward|cashback|paisa|paise)\\b")) action = true;
            if (has(sentence, "\\b(?:update.{0,15}kyc|verify.{0,20}account|login|log in|install.{0,25}(?:app|apk)|download.{0,25}(?:app|apk)|screen shar(?:e|ing)|remote access)\\b")) sensitive = true;
            boolean paymentAdvice = has(sentence, "\\b(?:do not|don't|never|must not)\\b.{0,25}\\b(?:pay|send|transfer)\\b");
            if (!paymentAdvice && has(sentence, "\\b(?:pay|send|transfer)\\b.{0,55}\\b(?:receive|claim|unlock|get)\\b.{0,35}\\b(?:refund|reward|cashback|prize)\\b")) payToReceive = true;
            boolean installAdvice = has(sentence, "\\b(?:do not|don't|never|must not)\\b.{0,25}\\b(?:install|download)\\b");
            if (!installAdvice && has(sentence, "\\b(?:install|download)\\b.{0,50}\\b(?:apk|remote access|anydesk|teamviewer)\\b")) remoteApp = true;
            boolean collectAdvice = has(sentence,"\\b(?:do not|don't|never|must not)\\b.{0,25}\\b(?:approve|accept|authorise|authorize|scan)\\b");
            if(!collectAdvice && has(sentence,"\\b(?:approve|accept|authorise|authorize)\\b.{0,40}\\b(?:collect|payment|upi)\\b.{0,50}\\b(?:receive|get|claim|unlock)\\b.{0,30}\\b(?:money|refund|cashback|reward)\\b")) collectToReceive=true;
            if(!collectAdvice && has(sentence,"\\bscan\\b.{0,25}\\bqr\\b.{0,40}\\b(?:receive|get|claim)\\b.{0,25}\\b(?:money|refund|cashback|reward)\\b")) qrToReceive=true;
        }
        if (secret) out.add(new Finding("SECRET_REQUEST", Kind.RISK, "The text appears to ask you to disclose or enter an OTP, password, PIN, or card security code. Check who is asking."));
        if (urgent) out.add(new Finding("PRESSURE", Kind.RISK, "The text uses urgency or an account threat. Urgency alone does not prove a scam."));
        if (payToReceive)
            out.add(new Finding("PAY_TO_RECEIVE", Kind.RISK, "The text asks for a payment to receive a refund or reward. Verify the request independently."));
        if (remoteApp)
            out.add(new Finding("REMOTE_APP_REQUEST", Kind.RISK, "The text asks you to install an APK or remote-access tool. Verify through the organisation's official app."));
        if(collectToReceive) out.add(new Finding("UPI_COLLECT_TO_RECEIVE",Kind.RISK,"The text asks you to approve a payment or collect request to receive money. Review the transaction direction in your own payment app; approval can authorise a debit."));
        if(qrToReceive) out.add(new Finding("QR_TO_RECEIVE",Kind.RISK,"The text asks you to scan a QR code to receive money or a refund. Verify independently and read the payment screen before approving anything."));
        return new Signals(urgent, secret, action, sensitive, List.copyOf(out));
    }
}
