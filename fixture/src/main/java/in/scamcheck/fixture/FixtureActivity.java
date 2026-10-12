package in.scamcheck.fixture;
import android.app.*;
import android.os.Bundle;
import android.widget.TextView;
public final class FixtureActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);TextView label=new TextView(this);label.setText("Synthetic notification sender for ScamCheck testing. No real messages are used.");label.setTextSize(20);setContentView(label);
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel("fixtures","Synthetic test messages",NotificationManager.IMPORTANCE_DEFAULT));
        String body="scam".equals(getIntent().getStringExtra("case"))?"Urgent! Share your OTP 123456 with our agent immediately":"Your order has shipped. No action needed.";
        manager.notify(501,new Notification.Builder(this,"fixtures").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Synthetic test preview").setContentText(body).build());finish();
    }
}
