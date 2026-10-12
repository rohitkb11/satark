package in.scamcheck.app;
import android.app.*;
import android.content.*;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import in.scamcheck.core.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;

@RunWith(AndroidJUnit4.class)
public class Phase3DeviceTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    @Before public void reset()throws Exception{new LocalSettings(context).history(false);new LocalSettings(context).monitoring(false);new EncryptedHistory(context).clear();PreviewAlerts.clear();((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(PreviewListener.ALERT_ID);}
    @After public void cleanup()throws Exception{new LocalSettings(context).history(false);new LocalSettings(context).monitoring(false);new LocalSettings(context).sources(java.util.Set.of(LocalSettings.WHATSAPP,LocalSettings.BUSINESS));new EncryptedHistory(context).clear();PreviewAlerts.clear();}
    @Test public void historyIsEncryptedMinimalAndTamperEvident()throws Exception{
        LocalSettings settings=new LocalSettings(context);EncryptedHistory history=new EncryptedHistory(context);
        CheckResult result=new ScamChecker().check("PRIVATE_MESSAGE_TOKEN https://example.org/account?secret=PRIVATE_QUERY_TOKEN");
        history.save(result);assertEquals(0,history.entries().length());settings.history(true);history.save(result);
        JSONArray entries=history.entries();assertEquals(1,entries.length());String decoded=entries.toString();
        assertFalse(decoded.contains("PRIVATE_MESSAGE_TOKEN"));assertFalse(decoded.contains("PRIVATE_QUERY_TOKEN"));
        File file=new File(context.getNoBackupFilesDir(),"history.enc");byte[] bytes;
        try(InputStream in=new FileInputStream(file)){bytes=SignedUpdate.readBounded(in,256*1024);}
        assertFalse(new String(bytes,StandardCharsets.ISO_8859_1).contains("example.org"));
        // Recover a previous committed file after an interrupted AtomicFile replacement.
        assertTrue(file.renameTo(new File(file.getPath()+".bak")));assertEquals(1,history.entries().length());assertTrue(file.exists());
        bytes[bytes.length-1]^=1;try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);}
        boolean rejected=false;try{history.entries();}catch(Exception e){rejected=true;}assertTrue(rejected);
        settings.history(false);history.clear();assertFalse(file.exists());
    }
    @Test public void updateActivationRejectsTamperAndKeepsReplayFloorAfterRollback()throws Exception{
        // Fixture-owned data only, on an emulator/test install. Reset generated update files for repeatability.
        File dir=new File(context.getNoBackupFilesDir(),"updates");File[] old=dir.listFiles();if(old!=null)for(File file:old)if(file.isFile())assertTrue(file.delete());
        UpdateStore store=new UpdateStore(context);byte[] first=asset("valid-v1.scu"),second=asset("valid-v2.scu");
        assertEquals(1,store.importUpdate(new ByteArrayInputStream(first)).version());assertEquals(1,store.current().version());
        byte[] bad=second.clone();bad[bad.length-1]^=1;boolean rejected=false;try{store.importUpdate(new ByteArrayInputStream(bad));}catch(Exception e){rejected=true;}assertTrue(rejected);assertEquals(1,store.current().version());
        assertEquals(2,store.importUpdate(new ByteArrayInputStream(second)).version());assertEquals(1,store.rollback().version());
        rejected=false;try{store.importUpdate(new ByteArrayInputStream(first));}catch(Exception e){rejected=true;}assertTrue(rejected);assertTrue(EngineFactory.create(context).modelAvailable());
    }
    @Test public void routedLinkIsConfirmedAndNotOpenedAutomatically()throws Exception{
        Intent intent=new Intent(context,MainActivity.class).setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse("https://www.amazon.in/orders")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)instrumentation.startActivitySync(intent);
        try{instrumentation.runOnMainSync(()->{assertEquals("https://www.amazon.in/orders",((android.widget.EditText)activity.findViewById(MainActivity.INPUT_ID)).getText().toString());assertEquals(View.GONE,((View)activity.findViewById(MainActivity.RESULT_ID).getParent()).getVisibility());});}
        finally{instrumentation.runOnMainSync(activity::finish);}
    }
    @Test public void renderPrivacyControlsWithDefaultsOff()throws Exception{
        PrivacyActivity activity=(PrivacyActivity)instrumentation.startActivitySync(new Intent(context,PrivacyActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{Thread.sleep(500);android.graphics.Bitmap[] bitmap={null};instrumentation.runOnMainSync(()->{
            assertFalse(((android.widget.CheckBox)activity.findViewById(PrivacyActivity.MONITOR_ID)).isChecked());
            assertFalse(((android.widget.CheckBox)activity.findViewById(PrivacyActivity.HISTORY_ID)).isChecked());
            View root=activity.getWindow().getDecorView();bitmap[0]=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap[0]));
        });try(var out=new FileOutputStream(new File(context.getCacheDir(),"ScamCheck-controls.png"))){bitmap[0].compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap[0].recycle();}
        finally{instrumentation.runOnMainSync(activity::finish);}
    }
    @Test public void previewServiceWarnsOnlyAfterConsentAndSourceSelection()throws Exception{
        LocalSettings settings=new LocalSettings(context);settings.sources(java.util.Set.of("in.scamcheck.fixture"));
        launchFixture("scam");Thread.sleep(1000);assertNull(findWarning());
        // Instrumentation force-stops the target and kills its old system binding. Toggle access
        // inside this emulator-only test after the instrumented process has started.
        shell("settings put secure enabled_notification_listeners null");
        Thread.sleep(300);
        shell("settings put secure enabled_notification_listeners in.scamcheck.app/in.scamcheck.app.PreviewListener");
        settings.monitoring(true);android.service.notification.NotificationListenerService.requestRebind(new ComponentName(context,PreviewListener.class));
        for(int i=0;i<100 && !PreviewListener.connectedForProcess;i++)Thread.sleep(100);
        assertTrue("Android notification access must be enabled for this emulator test",PreviewListener.connectedForProcess);launchFixture("scam");
        Notification warning=null;for(int i=0;i<100;i++){warning=findWarning();if(warning!=null)break;Thread.sleep(100);}
        assertNotNull("System notification listener did not deliver a selected preview",warning);
        assertFalse(warning.extras.toString().contains("123456"));assertNotNull(warning.publicVersion);
        assertFalse(warning.publicVersion.extras.toString().contains("password"));
        settings.monitoring(false);((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(PreviewListener.ALERT_ID);launchFixture("scam");Thread.sleep(1000);assertNull(findWarning());
    }
    private Notification findWarning(){for(var item:((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications())if(item.getId()==PreviewListener.ALERT_ID)return item.getNotification();return null;}
    private void shell(String command)throws Exception{try(var descriptor=instrumentation.getUiAutomation().executeShellCommand(command);var input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)){while(input.read()!=-1){}}}
    private void launchFixture(String mode){Intent fixture=new Intent().setComponent(new ComponentName("in.scamcheck.fixture","in.scamcheck.fixture.FixtureActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("case",mode);context.startActivity(fixture);}
    private byte[] asset(String name)throws Exception{try(InputStream in=instrumentation.getContext().getAssets().open(name)){return SignedUpdate.readBounded(in,SignedUpdate.MAX_BYTES);}}
}
