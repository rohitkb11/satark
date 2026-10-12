package in.scamcheck.cli;
import in.scamcheck.core.*;
import java.nio.file.*;
import java.util.*;

/** Development evaluation only: no messages are printed. */
public final class Evaluate {
    public static void main(String[] args) throws Exception {
        AssistedChecker engine=new AssistedChecker();
        int[][] counts=new int[2][4]; int n=0;
        for(String line:Files.readAllLines(Path.of(args[0]))){
            String[] fields=line.split("\t",5);
            if(fields.length!=5 || !fields[2].equals("test"))continue;
            boolean truth=fields[3].equals("1");n++;
            for(int mode=0;mode<2;mode++){
                boolean review=engine.check(fields[4],mode==1,false).level()!=CheckResult.Level.NO_WARNING_SIGNS;
                counts[mode][truth?(review?0:3):(review?1:2)]++;
            }
        }
        System.out.println("Synthetic held-out scenarios: "+n+". Review means Needs checking or Strong warning; not fraud proof.");
        for(int mode=0;mode<2;mode++){
            int[] c=counts[mode];
            System.out.printf(Locale.ROOT,"%s: TP=%d FP=%d TN=%d FN=%d; review precision=%.3f; recall=%.3f; benign review rate=%.3f%n",
                mode==0?"Rules only":"Rules + experimental model",c[0],c[1],c[2],c[3],
                c[0]/(double)Math.max(1,c[0]+c[1]),c[0]/(double)Math.max(1,c[0]+c[3]),c[1]/(double)Math.max(1,c[1]+c[2]));
        }
    }
}
