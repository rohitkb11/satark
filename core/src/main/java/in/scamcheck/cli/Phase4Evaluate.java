package in.scamcheck.cli;
import in.scamcheck.core.*;
import java.nio.file.*;
import java.util.*;
import java.security.MessageDigest;

/** Fixed, authored synthetic evaluation. Never used to retrain or choose a threshold. */
public final class Phase4Evaluate {
    public static void main(String[] args)throws Exception{
        Path dataset=Path.of(args[0]),output=Path.of(args[1]);
        var lines=Files.readAllLines(dataset);long start=System.nanoTime();AssistedChecker checker=new AssistedChecker();
        double cold=(System.nanoTime()-start)/1e6;StringBuilder cases=new StringBuilder();String[] reports=new String[2];
        for(int mode=0;mode<2;mode++){
            int tp=0,fp=0,tn=0,fn=0,unknown=0,uncertain=0,strongBenign=0;List<Double> times=new ArrayList<>();
            for(int warm=0;warm<10;warm++)checker.check("Your account balance was updated.",mode==1,false);
            for(String line:lines.subList(1,lines.size())){
                String[] f=line.split("\t",4);start=System.nanoTime();CheckResult result=checker.check(f[3],mode==1,false);times.add((System.nanoTime()-start)/1e6);
                boolean review=result.level()!=CheckResult.Level.NO_WARNING_SIGNS;
                if(f[2].equals("scam")){if(review)tp++;else fn++;}
                else if(f[2].equals("benign")){if(review)fp++;else tn++;if(result.level()==CheckResult.Level.STRONG_WARNING)strongBenign++;}
                else unknown++;
                if(result.level()==CheckResult.Level.NEEDS_CHECKING)uncertain++;
                if(cases.length()>0)cases.append(',');
                cases.append(String.format(Locale.ROOT,"{\"id\":\"%s\",\"label\":\"%s\",\"mode\":\"%s\",\"level\":\"%s\"}",f[0],f[2],mode==0?"rules":"assisted",result.level()));
            }
            Collections.sort(times);reports[mode]=String.format(Locale.ROOT,"{\"mode\":\"%s\",\"tp\":%d,\"fp\":%d,\"tn\":%d,\"fn\":%d,\"precision\":%.4f,\"recall\":%.4f,\"false_reviews_per_1000_benign\":%.2f,\"strong_warnings_on_benign\":%d,\"unknown_excluded_from_binary_metrics\":%d,\"needs_checking_count\":%d,\"p50_ms\":%.3f,\"p95_ms\":%.3f}",mode==0?"rules":"assisted",tp,fp,tn,fn,tp/(double)Math.max(1,tp+fp),tp/(double)Math.max(1,tp+fn),fp*1000.0/Math.max(1,fp+tn),strongBenign,unknown,uncertain,times.get(times.size()/2),times.get((int)Math.ceil(times.size()*0.95)-1));
        }
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(dataset)));
        String json="{\"dataset_sha256\":\""+hash+"\",\"count\":"+(lines.size()-1)+",\"warning\":\"AI-assisted synthetic engineering scenarios, authored with knowledge of the implementation; not independent or real-world accuracy. Review combines Needs checking and Strong warning.\",\"cold_init_ms\":"+cold+",\"modes\":["+String.join(",",reports)+"],\"cases\":["+cases+"]}";
        Files.createDirectories(output.toAbsolutePath().getParent());Files.writeString(output,json);System.out.println("Phase 4 synthetic evidence written; original text omitted from results.");
    }
}
