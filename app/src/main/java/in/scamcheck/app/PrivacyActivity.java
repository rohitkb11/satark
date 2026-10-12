package in.scamcheck.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.text.DateFormat;

public final class PrivacyActivity extends Activity {
    public static final int MONITOR_ID=2001,HISTORY_ID=2002,IMPORT_ID=2003,ROLLBACK_ID=2004,HISTORY_VIEW_ID=2005;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private LocalSettings prefs;private TextView status,updateStatus;private CheckBox monitoring,history;private boolean binding,destroyed;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);prefs=new LocalSettings(this);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xffF5F6F1);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(32,48,32,40);scroll.addView(page);setContentView(scroll);
        if(Build.VERSION.SDK_INT>=30)page.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(32+bars.left,48+bars.top,32+bars.right,40+bars.bottom);return insets;});
        label(page,"Privacy and connections",26,true);
        label(page,"All checks stay on this phone. These features are optional.",15,false);
        label(page,"Notification previews",20,true);
        label(page,"Android notification access can expose notifications from every app. ScamCheck filters first and reads body text only from selected WhatsApp apps. It never reads chats or SMS inboxes. Hidden or incomplete previews cannot be fully checked.",14,false);
        Set<String> selected=prefs.sources();source(page,"WhatsApp",LocalSettings.WHATSAPP,selected);source(page,"WhatsApp Business",LocalSettings.BUSINESS,selected);
        monitoring=toggle(page,"Check selected notification previews",MONITOR_ID);
        monitoring.setOnCheckedChangeListener((button,enabled)->{
            if(binding)return;
            if(!enabled){prefs.monitoring(false);clearAlerts();refresh();return;}
            binding=true;monitoring.setChecked(false);binding=false;
            if(prefs.sources().isEmpty()){status.setText("Select at least one WhatsApp source first.");return;}
            secure(new AlertDialog.Builder(this).setTitle("Allow preview checks?").setMessage("Android will offer broad notification access. ScamCheck will process only body text exposed by your selected WhatsApp apps, in memory on this phone. It warns only for strong rule evidence. It does not save previews or upload content.\n\nThis is a prototype, not a guarantee against scams. Notification content may be missing or redacted. You can stop checks here or revoke Android access at any time.")
                .setNegativeButton("Not now",null).setPositiveButton("I agree",(d,w)->{
                    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},41);
                    else enableMonitoring();
                }).create());
        });
        button(page,"Open Android notification access",()->openAccess());
        label(page,"Review details expire after five minutes. Warnings hide message content on the lock screen. Preview checks are rules-only; experimental AI is not used automatically.",13,false);
        label(page,"Encrypted history",20,true);
        label(page,"Off by default. When enabled, only summaries of checks you run yourself are saved: time, outcome, website addresses and reason codes. Original messages and complete links are never saved. Preview checks are excluded.",14,false);
        history=toggle(page,"Save manual check summaries",HISTORY_ID);
        history.setOnCheckedChangeListener((button,enabled)->{
            if(binding)return;
            if(enabled){binding=true;history.setChecked(false);binding=false;secure(new AlertDialog.Builder(this).setTitle("Save encrypted summaries?").setMessage("Website addresses and risk reasons may reveal browsing interests. Summaries stay encrypted with a key held by Android Keystore, are excluded from backups, and can be deleted. Only your manual checks are saved.")
                .setNegativeButton("Cancel",(d,w)->refresh()).setPositiveButton("Enable",(d,w)->{prefs.history(true);refresh();}).create());}
            else{prefs.history(false);worker.execute(()->{try{new EncryptedHistory(this).clear();message("History disabled and deleted.");}catch(Exception e){message("History disabled. Deletion needs attention; use Delete all.");}});}
        });
        Button view=button(page,"View encrypted history",()->showHistory());view.setId(HISTORY_VIEW_ID);
        button(page,"Delete all history",()->secure(new AlertDialog.Builder(this).setTitle("Delete all summaries?").setMessage("This removes saved summaries and their encryption key.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->worker.execute(()->{try{new EncryptedHistory(this).clear();message("All saved summaries deleted.");}catch(Exception e){message("History could not be deleted. Please retry.");}})).create()));
        label(page,"Signed offline updates",20,true);
        label(page,"Import a local .scu file from the prototype publisher. Signatures check its origin, not the truth of its data. Only website lists and model weights can change. Older, damaged, expired or untrusted updates are rejected.",14,false);
        updateStatus=label(page,"Checking saved data...",14,false);worker.execute(()->refreshUpdates());
        Button importButton=button(page,"Import signed update",()->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");try{startActivityForResult(pick,51);}catch(ActivityNotFoundException e){message("No file picker is available. Install a local document provider.");}});importButton.setId(IMPORT_ID);
        Button rollback=button(page,"Restore previous data",()->secure(new AlertDialog.Builder(this).setTitle("Restore previous data?").setMessage("The previous signed, unexpired data will be restored. The anti-replay version limit stays in place.").setNegativeButton("Cancel",null).setPositiveButton("Restore",(d,w)->worker.execute(()->{try{new UpdateStore(this).rollback();message("Previous signed data restored.");}catch(Exception e){message("No valid previous data could be restored.");}refreshUpdates();})).create()));rollback.setId(ROLLBACK_ID);
        status=label(page,"",14,false);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        button(page,"Back to checking",()->finish());refresh();
    }
    private void source(LinearLayout parent,String title,String pkg,Set<String> initial){CheckBox box=toggle(parent,title,View.generateViewId());box.setChecked(initial.contains(pkg));box.setOnCheckedChangeListener((b,on)->{Set<String> chosen=prefs.sources();if(on)chosen.add(pkg);else chosen.remove(pkg);prefs.sources(chosen);if(chosen.isEmpty()){prefs.monitoring(false);clearAlerts();refresh();}});}
    private void enableMonitoring(){prefs.monitoring(true);refresh();try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,PreviewListener.class));}catch(SecurityException ignored){}openAccess();}
    private void openAccess(){try{startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(ActivityNotFoundException e){message("Notification access settings are unavailable on this device.");}}
    private void clearAlerts(){PreviewAlerts.clear();((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(PreviewListener.ALERT_ID);}
    private void refresh(){binding=true;monitoring.setChecked(prefs.monitoring());history.setChecked(prefs.history());binding=false;}
    private void refreshUpdates(){runOnUiThread(()->{if(!destroyed)updateStatus.setText(new UpdateStore(this).status());});}
    @Override protected void onResume(){super.onResume();if(status!=null){refresh();String listeners=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");boolean granted=listeners!=null && Arrays.stream(listeners.split(":")).anyMatch(s->new ComponentName(this,PreviewListener.class).equals(ComponentName.unflattenFromString(s)));boolean alerts=Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;status.setText(prefs.monitoring()?(!alerts?"Alerts are blocked; preview checks are paused.":granted?"Preview checks enabled for your selected sources.":"Consent saved. Enable ScamCheck in Android notification access to start."):"Automatic preview checks are off.");}}
    @Override public void onRequestPermissionsResult(int request,String[] names,int[] grants){super.onRequestPermissionsResult(request,names,grants);if(request==41){if(grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)enableMonitoring();else{prefs.monitoring(false);refresh();message("Preview checks remain off because alerts were not allowed.");}}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=51 || result!=RESULT_OK || data==null || data.getData()==null)return;Uri uri=data.getData();if(!"content".equals(uri.getScheme())){message("Choose a local file through the document picker.");return;}
        worker.execute(()->{try(java.io.InputStream input=getContentResolver().openInputStream(uri)){if(input==null)throw new java.io.IOException();in.scamcheck.core.SignedUpdate.Bundle bundle=new UpdateStore(this).importUpdate(input);message("Signed data v"+bundle.version()+" imported. Experimental AI stays opt-in.");}catch(Exception e){message("Update rejected. Check its signature, version, expiry and contents. Current data was kept.");}refreshUpdates();});
    }
    private void showHistory(){worker.execute(()->{try{JSONArray entries=new EncryptedHistory(this).entries();runOnUiThread(()->{if(destroyed)return;if(entries.length()==0){message("No saved summaries.");return;}String[] labels=new String[entries.length()];for(int i=0;i<labels.length;i++){JSONObject entry=entries.optJSONObject(i);labels[i]=DateFormat.getDateTimeInstance().format(new Date(entry.optLong("time")))+" · "+entry.optString("level");}
            secure(new AlertDialog.Builder(this).setTitle("Manual check summaries").setItems(labels,(d,index)->{JSONObject entry=entries.optJSONObject(index);secure(new AlertDialog.Builder(this).setTitle(entry.optString("level")).setMessage("Method: "+entry.optString("method")+"\nWebsite addresses: "+entry.optJSONArray("domains")+"\nReason codes: "+entry.optJSONArray("codes")+"\n\nNo original message or full link was saved.").setPositiveButton("Close",null).setNegativeButton("Delete",(dialog,which)->worker.execute(()->{try{new EncryptedHistory(this).delete(entry.optString("id"));message("Summary deleted.");}catch(Exception e){message("Summary could not be deleted.");}})).create());}).setPositiveButton("Close",null).create());});}catch(Exception e){message("History could not be read safely. You can delete it without opening it.");}});}
    private void message(String value){runOnUiThread(()->{if(!destroyed)status.setText(value);});}
    private void secure(AlertDialog dialog){dialog.setOnDismissListener(d->refresh());dialog.show();dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
    private TextView label(LinearLayout parent,String value,int size,boolean bold){TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(0xff17332C);if(bold)text.setTypeface(null,android.graphics.Typeface.BOLD);text.setPadding(8,18,8,14);parent.addView(text);return text;}
    private CheckBox toggle(LinearLayout parent,String text,int id){CheckBox box=new CheckBox(this);box.setText(text);box.setId(id);box.setSaveEnabled(false);parent.addView(box);return box;}
    private Button button(LinearLayout parent,String text,Runnable action){Button button=new Button(this);button.setText(text);button.setAllCaps(false);parent.addView(button);button.setOnClickListener(v->action.run());return button;}
    @Override protected void onDestroy(){destroyed=true;worker.shutdown();super.onDestroy();}
}
