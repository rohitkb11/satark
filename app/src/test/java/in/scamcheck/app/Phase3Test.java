package in.scamcheck.app;
import android.app.*;
import android.content.*;
import android.os.Looper;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class Phase3Test {
    private ActivityController<PrivacyActivity> controls;
    @After public void cleanup(){if(controls!=null)controls.pause().stop().destroy();}
    @Test public void defaultsAndHistoryConsentCancellation(){
        controls=Robolectric.buildActivity(PrivacyActivity.class).setup();PrivacyActivity activity=controls.get();LocalSettings settings=new LocalSettings(activity);
        assertFalse(settings.monitoring());assertFalse(settings.history());
        ((CheckBox)activity.findViewById(PrivacyActivity.HISTORY_ID)).setChecked(true);
        AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();assertTrue(dialog.isShowing());
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(settings.history());assertFalse(((CheckBox)activity.findViewById(PrivacyActivity.HISTORY_ID)).isChecked());
    }
    @Test public void historyRequiresPositiveConsent(){
        controls=Robolectric.buildActivity(PrivacyActivity.class).setup();PrivacyActivity activity=controls.get();
        ((CheckBox)activity.findViewById(PrivacyActivity.HISTORY_ID)).setChecked(true);assertFalse(new LocalSettings(activity).history());
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(new LocalSettings(activity).history());
    }
    @Test public void notificationConsentCannotBeSkipped(){
        controls=Robolectric.buildActivity(PrivacyActivity.class).setup();PrivacyActivity activity=controls.get();
        ((CheckBox)activity.findViewById(PrivacyActivity.MONITOR_ID)).setChecked(true);
        assertFalse(new LocalSettings(activity).monitoring());
        org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        assertFalse(new LocalSettings(activity).monitoring());
    }
    @Test public void linkGateRejectsAmbiguity(){
        assertTrue(LinkGate.openable("https://www.amazon.in/orders"));
        assertFalse(LinkGate.openable("https://amazon.in@evil.com"));
        assertFalse(LinkGate.openable("javascript:alert(1)"));
        assertFalse(LinkGate.openable("https://аmazon.in"));
        assertFalse(LinkGate.openable("https://amazon.in\\@evil.com"));
        assertFalse(LinkGate.openable("https://192.0.2.1/login"));
        assertFalse(LinkGate.openable("https://amazon.in:99999"));
    }
    @Test public void webViewIntentRequiresManualConfirmation()throws Exception{
        var controller=Robolectric.buildActivity(MainActivity.class,new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://www.amazon.in/orders"))).setup();
        try{MainActivity activity=controller.get();assertEquals("https://www.amazon.in/orders",((EditText)activity.findViewById(MainActivity.INPUT_ID)).getText().toString());
            assertEquals(android.view.View.GONE,((android.view.View)activity.findViewById(MainActivity.RESULT_ID).getParent()).getVisibility());assertNull(activity.getIntent().getData());
        }finally{controller.pause().stop().destroy();}
    }
    @Test public void previewDetailsSurviveReturningToExistingActivity()throws Exception{
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try{MainActivity activity=controller.get();controller.pause();
            String token=PreviewAlerts.put(new in.scamcheck.core.ScamChecker().check("Urgent! Share your OTP with our agent",true));
            activity.onNewIntent(new Intent().putExtra("preview_token",token));controller.resume();
            assertEquals(android.view.View.VISIBLE,((android.view.View)activity.findViewById(MainActivity.RESULT_ID).getParent()).getVisibility());
            assertEquals("Strong warning",((TextView)activity.findViewById(MainActivity.RESULT_ID)).getText().toString());
        }finally{controller.pause().stop().destroy();}
    }
}
