package in.scamcheck.app;

import android.app.Instrumentation;
import android.content.*;
import android.view.View;
import android.widget.*;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.io.FileOutputStream;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises the installed APK on Android, including real bundled resources/desugaring. */
@RunWith(AndroidJUnit4.class)
public class DeviceSmokeTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private MainActivity activity;
    @After public void close(){if(activity!=null)instrumentation.runOnMainSync(()->activity.finish());}
    private void launch(String shared)throws Exception{
        Intent intent=new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if(shared!=null)intent.setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,shared);
        activity=(MainActivity)instrumentation.startActivitySync(intent);
        await(()->activity.findViewById(MainActivity.MODEL_ID).isEnabled());
    }
    @Test public void sharedTextNeedsConfirmationThenChecks()throws Exception{
        launch("HDFC Bank: Update KYC immediately. Your account will be blocked. https://hdfc.bank.in.fake-site.com");
        instrumentation.runOnMainSync(()->{
            assertEquals(View.GONE,((View)activity.findViewById(MainActivity.RESULT_ID).getParent()).getVisibility());
            assertFalse(((CheckBox)activity.findViewById(MainActivity.MODEL_ID)).isChecked());
            activity.findViewById(MainActivity.CHECK_ID).performClick();
        });
        awaitResult("Strong warning");
    }
    @Test public void normalOtpAndShortenedLink()throws Exception{
        launch(null);enterAndCheck("Your OTP is 123456. Do not share your OTP with anyone.");awaitResult("No warning signs found");
        enterAndCheck("https://bit.ly/example");awaitResult("Needs checking");
    }
    @Test public void experimentalModelAndClear()throws Exception{
        launch("For account recovery our officer needs your login secret in this conversation.");
        instrumentation.runOnMainSync(()->{
            ((CheckBox)activity.findViewById(MainActivity.MODEL_ID)).setChecked(true);
            activity.findViewById(MainActivity.CHECK_ID).performClick();
        });
        awaitResult("Needs checking");
        instrumentation.runOnMainSync(()->{
            activity.findViewById(MainActivity.CLEAR_ID).performClick();
            assertEquals(0,((EditText)activity.findViewById(MainActivity.INPUT_ID)).length());
            assertEquals(View.GONE,((View)activity.findViewById(MainActivity.RESULT_ID).getParent()).getVisibility());
        });
    }
    @Test public void editCancelsPendingResultAndAppHasNoPermissions()throws Exception{
        launch(null);
        instrumentation.runOnMainSync(()->{
            EditText input=activity.findViewById(MainActivity.INPUT_ID);input.setText("Urgent! Share your OTP");
            activity.findViewById(MainActivity.CHECK_ID).performClick();input.setText("Different message");
        });
        Thread.sleep(500);instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->assertEquals(View.GONE,((View)activity.findViewById(MainActivity.RESULT_ID).getParent()).getVisibility()));
        String[] permissions=activity.getPackageManager().getPackageInfo(activity.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions;
        assertArrayEquals(new String[]{"android.permission.POST_NOTIFICATIONS"},permissions);
    }
    @Test public void renderSyntheticHomePreview()throws Exception{
        launch(null);saveSyntheticPreview("ScamCheck-home.png");
    }
    @Test public void renderSyntheticResultPreview()throws Exception{
        launch("HDFC Bank: Your account will be blocked. Update KYC immediately at https://hdfc.bank.in.fake-site.com/verify");
        instrumentation.runOnMainSync(()->activity.findViewById(MainActivity.CHECK_ID).performClick());
        awaitResult("Strong warning");Thread.sleep(600);saveSyntheticPreview("ScamCheck-result.png");
    }
    private void saveSyntheticPreview(String filename)throws Exception{
        Bitmap[] bitmap={null};
        instrumentation.runOnMainSync(()->{
            View root=activity.getWindow().getDecorView();
            bitmap[0]=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);
            root.draw(new Canvas(bitmap[0]));
        });
        // Test APK only. Synthetic inputs only. No screenshot/export code in the user app.
        File file=new File(instrumentation.getTargetContext().getCacheDir(),filename);
        try(FileOutputStream out=new FileOutputStream(file)){bitmap[0].compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap[0].recycle();
    }
    private void enterAndCheck(String content){instrumentation.runOnMainSync(()->{
        ((EditText)activity.findViewById(MainActivity.INPUT_ID)).setText(content);activity.findViewById(MainActivity.CHECK_ID).performClick();
    });}
    private void awaitResult(String expected)throws Exception{await(()->{
        TextView result=activity.findViewById(MainActivity.RESULT_ID);
        return ((View)result.getParent()).getVisibility()==View.VISIBLE && expected.contentEquals(result.getText());
    });}
    private interface Condition{boolean ready();}
    private void await(Condition condition)throws Exception{
        for(int i=0;i<200;i++){
            boolean[] ready={false};instrumentation.runOnMainSync(()->ready[0]=condition.ready());
            if(ready[0])return;Thread.sleep(50);
        }
        fail("Android UI did not reach the expected state");
    }
}
