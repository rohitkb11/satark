package in.scamcheck.app;
import in.scamcheck.core.CheckResult;
import java.util.*;
import android.os.SystemClock;

/** Short-lived memory only. PendingIntent contains an unguessable token, not message text. */
final class PreviewAlerts {
    private record Entry(CheckResult result,long expires){}
    private static final Map<String,Entry> entries=new LinkedHashMap<>();
    static synchronized String put(CheckResult result){prune();while(entries.size()>=8)entries.remove(entries.keySet().iterator().next());String token=UUID.randomUUID().toString();entries.put(token,new Entry(result,SystemClock.elapsedRealtime()+5*60_000));return token;}
    static synchronized CheckResult take(String token){prune();Entry entry=entries.remove(token);return entry==null?null:entry.result;}
    static synchronized void clear(){entries.clear();}
    private static void prune(){entries.entrySet().removeIf(e->e.getValue().expires<SystemClock.elapsedRealtime());}
}
