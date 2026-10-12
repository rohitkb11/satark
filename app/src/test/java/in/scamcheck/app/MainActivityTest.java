package in.scamcheck.app;

import android.content.*;
import android.content.pm.PackageInfo;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    @After public void close(){if(controller!=null)controller.pause().stop().destroy();}
    private void launch(Intent intent)throws Exception{
        controller=Robolectric.buildActivity(MainActivity.class,intent).setup();activity=controller.get();
        waitFor(()->activity.findViewById(MainActivity.MODEL_ID).isEnabled());
    }
    private EditText input(){return activity.findViewById(MainActivity.INPUT_ID);}
    private TextView result(){return activity.findViewById(MainActivity.RESULT_ID);}
    private void check()throws Exception{activity.findViewById(MainActivity.CHECK_ID).performClick();waitFor(()->result().getParent()!=null && ((View)result().getParent()).getVisibility()==View.VISIBLE);}
    @Test public void shareRequiresConfirmationAndChecksLocally()throws Exception{
        Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Urgent! Share your OTP with our agent");
        launch(share);assertTrue(input().getText().toString().contains("Share your OTP"));
        assertEquals(View.GONE,((View)result().getParent()).getVisibility());
        assertFalse(((CheckBox)activity.findViewById(MainActivity.MODEL_ID)).isChecked());
        check();assertEquals("Strong warning",result().getText().toString());
        assertNull(activity.getIntent().getCharSequenceExtra(Intent.EXTRA_TEXT));
    }
    @Test public void normalOtpAndShortLinkHaveDifferentResults()throws Exception{
        launch(new Intent());input().setText("Your OTP is 123456. Do not share your OTP with anyone.");check();assertEquals("No warning signs found",result().getText().toString());
        input().setText("https://bit.ly/example");check();assertEquals("Needs checking",result().getText().toString());
    }
    @Test public void editingOrClearingInvalidatesOldResults()throws Exception{
        launch(new Intent());input().setText("Urgent! Share your OTP");check();
        input().setText("New text");assertEquals(View.GONE,((View)result().getParent()).getVisibility());
        activity.findViewById(MainActivity.CLEAR_ID).performClick();assertEquals("",input().getText().toString());
        assertFalse(activity.findViewById(MainActivity.CHECK_ID).isEnabled());
    }
    @Test public void experimentalAiRunsOnlyWhenSelected()throws Exception{
        launch(new Intent());input().setText("For account recovery our officer needs your login secret in this conversation.");
        ((CheckBox)activity.findViewById(MainActivity.MODEL_ID)).setChecked(true);check();
        assertEquals("Needs checking",result().getText().toString());
    }
    @Test public void processSelectedTextIsReceivedWithoutAutoAnalysis()throws Exception{
        launch(new Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain").putExtra(Intent.EXTRA_PROCESS_TEXT,"Check this selected message"));
        assertEquals("Check this selected message",input().getText().toString());
        assertEquals(View.GONE,((View)result().getParent()).getVisibility());
    }
    @Test public void privacyAndManifestBoundaries()throws Exception{
        launch(new Intent());PackageInfo info=activity.getPackageManager().getPackageInfo(activity.getPackageName(),android.content.pm.PackageManager.GET_PERMISSIONS);
        assertArrayEquals(new String[]{"android.permission.POST_NOTIFICATIONS"},info.requestedPermissions);
        assertFalse(input().isSaveEnabled());assertTrue((activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE)!=0);
        assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,input().getImportantForAutofill());
    }
    private interface Condition{boolean ready();}
    private void waitFor(Condition condition)throws Exception{
        for(int i=0;i<150;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();if(condition.ready())return;Thread.sleep(20);}
        fail("UI did not reach the expected state");
    }
}
