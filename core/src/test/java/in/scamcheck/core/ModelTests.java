package in.scamcheck.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static in.scamcheck.core.CheckResult.*;

public final class ModelTests {
    public static void main(String[] args) throws Exception {
        LocalTextModel model = new LocalTextModel();
        double expected=0.9436055770218571;
        double actual=model.score("For account recovery our officer needs your login secret in this conversation.");
        require(Math.abs(expected-actual)<1e-9,"Python/Java inference parity");
        AssistedChecker assisted=new AssistedChecker();
        require(assisted.modelAvailable(),"Bundled model loads");
        CheckResult suggestion=assisted.check("For account recovery our officer needs your login secret in this conversation.",true,false);
        require(suggestion.level()==Level.NEEDS_CHECKING,"Model alone never issues a strong warning");
        require(suggestion.method().equals("rules + experimental local model"),"Actual method disclosed");
        require(assisted.check("Urgent! Share your OTP with our agent",true,false).level()==Level.STRONG_WARNING,"Model cannot clear strong rule evidence");
        require(assisted.check("अपना खाता जांचें",true,false).method().contains("skipped"),"Unsupported script skips English model");
        require(assisted.check("plain text",false,false).method().equals("rules-only"),"AI disabled truly uses only rules");
        AssistedChecker missing=new AssistedChecker(new ScamChecker(),null);
        CheckResult fallback=missing.check("Hello",true,false);
        require(fallback.level()==Level.NEEDS_CHECKING && fallback.method().contains("unavailable"),"Failed model is visible, not a silent all-clear");
        boolean rejected=false;
        try{new LocalTextModel(new ByteArrayInputStream("wrong-format\n".getBytes(StandardCharsets.UTF_8)));}catch(IOException e){rejected=true;}
        require(rejected,"Malformed model fails clearly");
        require(!suggestion.toString().contains("login secret in this conversation"),"Result does not retain message text");
        System.out.println("PASS: 10 model integration checks, including Python/Java feature and score parity.");
    }
    private static void require(boolean condition,String label){if(!condition)throw new AssertionError(label);}
}
