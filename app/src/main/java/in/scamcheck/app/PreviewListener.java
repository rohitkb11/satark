package in.scamcheck.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import android.service.notification.*;
import in.scamcheck.core.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class PreviewListener extends NotificationListenerService {
    static final String CHANNEL="preview_warnings";
    static final int ALERT_ID=301;
    // Bound queued previews as well as the dedup cache; drop old pending previews during bursts.
    private final ExecutorService worker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(8),new ThreadPoolExecutor.DiscardOldestPolicy());
    private final Map<String,Long> seen=new LinkedHashMap<>();
    private long lastAlert=-120_000;
    private volatile boolean connected;
    static volatile boolean connectedForProcess;
    private AssistedChecker cached;private String cachedTag="";
    private boolean permitted(String source){LocalSettings settings=new LocalSettings(this);return connected && settings.monitoring() && settings.sources().contains(source)
        && (Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED);}
    @Override public void onNotificationPosted(StatusBarNotification item){
        if(item==null || item.getPackageName().equals(getPackageName()) || !permitted(item.getPackageName()))return;
        Notification notification=item.getNotification();if((notification.flags & Notification.FLAG_GROUP_SUMMARY)!=0)return;
        // Read body only; sender names, titles and full conversations are not accessed.
        CharSequence body;
        try{body=notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
            if(body==null)body=notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        }catch(RuntimeException e){return;}
        if(body==null || body.length()==0)return;
        String preview=body.subSequence(0,Math.min(body.length(),ScamChecker.MAX_CHARACTERS+1)).toString();String source=item.getPackageName();
        worker.execute(()->{
            if(!permitted(source))return;
            try{
                long now=SystemClock.elapsedRealtime();String digest=Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest((source+"\n"+preview).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                seen.entrySet().removeIf(e->now-e.getValue()>10*60_000);
                if(seen.containsKey(digest))return;while(seen.size()>=64)seen.remove(seen.keySet().iterator().next());seen.put(digest,now);
                String tag=new UpdateStore(this).activeId()+java.time.LocalDate.now();
                if(cached==null || !tag.equals(cachedTag)){cached=EngineFactory.create(this);cachedTag=tag;}
                CheckResult result=cached.check(preview,false,true);
                if(result.level()!=CheckResult.Level.STRONG_WARNING || now-lastAlert<120_000 || !permitted(source))return;
                lastAlert=now;String token=PreviewAlerts.put(result);
                NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Suspicious message previews",NotificationManager.IMPORTANCE_DEFAULT));
                Intent review=new Intent(this,MainActivity.class).putExtra("preview_token",token);
                PendingIntent action=PendingIntent.getActivity(this,301,review,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                Notification publicView=new Notification.Builder(this,CHANNEL).setSmallIcon(in.scamcheck.app.R.drawable.ic_shield).setContentTitle("ScamCheck").setContentText("Open the app to review a private warning.").build();
                Notification warning=new Notification.Builder(this,CHANNEL).setSmallIcon(in.scamcheck.app.R.drawable.ic_shield)
                    .setContentTitle("A message preview needs attention").setContentText("Tap to review the reasons. Only a preview was checked.")
                    .setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(publicView).setContentIntent(action).setAutoCancel(true).setTimeoutAfter(5*60_000).build();
                manager.notify(ALERT_ID,warning);
            }catch(Exception ignored){ /* No private text or exception details are logged. */ }
        });
    }
    @Override public void onListenerConnected(){connected=true;connectedForProcess=true;}
    @Override public void onListenerDisconnected(){connected=false;connectedForProcess=false;PreviewAlerts.clear();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(ALERT_ID);}
    @Override public void onDestroy(){connected=false;connectedForProcess=false;worker.shutdownNow();PreviewAlerts.clear();super.onDestroy();}
}
