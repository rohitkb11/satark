package in.scamcheck.app;
import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import in.scamcheck.core.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PerformanceTest {
    @Test public void measureOfflineEngineOnNamedEmulator()throws Exception{
        var context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        long start=System.nanoTime();AssistedChecker engine=new AssistedChecker();double cold=(System.nanoTime()-start)/1e6;
        long before=Debug.getPss();JSONArray modes=new JSONArray();
        String[] samples={"Your balance was updated.","Urgent! Share your OTP with the agent.","Approve this UPI collect request to receive your refund","HDFC Bank: update KYC at https://hdfc.bank.in.example.org/login","https://bit.ly/demo"};
        for(int mode=0;mode<2;mode++){
            List<Double> times=new ArrayList<>();for(int i=0;i<10;i++)engine.check(samples[i%samples.length],mode==1,false);
            for(int i=0;i<100;i++){start=System.nanoTime();CheckResult result=engine.check(samples[i%samples.length],mode==1,false);times.add((System.nanoTime()-start)/1e6);assertNotNull(result);}
            Collections.sort(times);modes.put(new JSONObject().put("mode",mode==0?"rules":"assisted").put("samples",100).put("p50_ms",times.get(50)).put("p95_ms",times.get(94)));
        }
        JSONObject report=new JSONObject().put("model",Build.MODEL).put("api",Build.VERSION.SDK_INT).put("fingerprint",Build.FINGERPRINT).put("abi",Build.SUPPORTED_ABIS[0]).put("cold_init_ms",cold).put("process_pss_before_kib",before).put("process_pss_after_kib",Debug.getPss()).put("modes",modes).put("note","Emulator measurements, instrumentation overhead included. Endpoint process PSS, not peak engine memory or physical-device battery evidence.");
        try(var out=new FileOutputStream(new File(context.getCacheDir(),"phase4-device.json"))){out.write(report.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
}
