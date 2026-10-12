package in.scamcheck.app;

import android.app.*;
import android.content.*;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.net.Uri;
import android.content.pm.ResolveInfo;
import in.scamcheck.core.*;
import java.util.*;
import java.util.concurrent.*;

/** Native offline UI. Shared text is confirmed before any analysis. */
public final class MainActivity extends Activity {
    public static final int INPUT_ID=1001, CHECK_ID=1002, CLEAR_ID=1003, CANCEL_ID=1004,
        RESULT_ID=1005, MODEL_ID=1006, STATUS_ID=1007, PRIVACY_ID=1008, PASTE_ID=1009;
    private static final int BG=0xffF5F6F1, INK=0xff17332C, MUTED=0xff5B6B65, GREEN=0xff146A53;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private AssistedChecker checker;
    private Future<?> pending;
    private long revision;
    private boolean destroyed, busy, partialInput, firstResume=true, freshPreview;
    private EditText input;
    private Button check, cancel;
    private CheckBox experimental;
    private TextView status, outcome;
    private LinearLayout results, evidence;
    private ScrollView scroll;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        buildScreen(); receive(getIntent());
        loadChecker();
    }
    private void loadChecker(){
        worker.execute(() -> {
            AssistedChecker loaded = null;
            try { loaded = EngineFactory.create(this); } catch (Exception ignored) { /* No input/error logging. */ }
            AssistedChecker finalLoaded = loaded;
            runOnUiThread(() -> {
                if (destroyed) return;
                checker = finalLoaded;
                if(results.getVisibility()!=View.VISIBLE)status.setText(checker == null ? "Checker unavailable. Close and reopen the app." : "Ready. Checks run only on this phone.");
                experimental.setEnabled(checker != null && checker.modelAvailable());
                if (checker != null && !checker.modelAvailable()) experimental.setText(R.string.model_unavailable);
                updateButtons();
            });
        });
    }
    @Override protected void onResume(){super.onResume();if(new LocalSettings(this).monitoring() && !PreviewListener.connectedForProcess){try{android.service.notification.NotificationListenerService.requestRebind(new ComponentName(this,PreviewListener.class));}catch(SecurityException ignored){}}if(firstResume){firstResume=false;freshPreview=false;return;}if(freshPreview){freshPreview=false;return;}invalidateCheck();checker=null;updateButtons();loadChecker();}
    private void buildScreen() {
        scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout page = vertical(); page.setPadding(dp(22), dp(16), dp(22), dp(28));
        scroll.addView(page); setContentView(scroll);
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                v.setPadding(dp(22)+bars.left, dp(16)+bars.top, dp(22)+bars.right, dp(28)+bars.bottom);
            }
            return insets;
        });
        TextView badge=text("ON YOUR PHONE  /  OFFLINE",11,GREEN,true); badge.setLetterSpacing(0.1f); page.addView(badge);
        page.addView(text("ScamCheck",34,INK,true), margin(0,12));
        page.addView(text("A second look before you act.",17,MUTED,false));
        page.addView(text("Check a message or link. Get clear reasons and a safer next step, without uploading your conversation.",14,MUTED,false),margin(12,20));

        LinearLayout entry=card(); page.addView(entry);
        entry.addView(text("What would you like to check?",18,INK,true));
        entry.addView(text("Paste text, or share it here from another app. Review the content before tapping Check.",13,MUTED,false),margin(7,12));
        input=new EditText(this); input.setId(INPUT_ID); input.setHint("Paste a suspicious message or website address...");
        input.setTextSize(16); input.setTextColor(INK); input.setHintTextColor(MUTED); input.setMinLines(4); input.setMaxLines(8);
        input.setGravity(Gravity.TOP); input.setPadding(dp(12),dp(12),dp(12),dp(12));
        input.setBackground(shape(0xffF6F8F5,12,0xffDDE5DE));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        input.setSaveEnabled(false); input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        // One extra code unit preserves a detectable truncation flag without retaining huge shared input.
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ScamChecker.MAX_CHARACTERS+1)});
        entry.addView(input);
        LinearLayout editing=row(); entry.addView(editing,margin(8,6));
        Button paste=button("Paste",false); paste.setId(PASTE_ID); editing.addView(paste,weight()); paste.setOnClickListener(v->paste());
        Button clear=button("Clear",false); clear.setId(CLEAR_ID); editing.addView(clear,weight());
        clear.setOnClickListener(v->{input.setText("");partialInput=false;status.setText(R.string.cleared);});
        experimental=new CheckBox(this); experimental.setId(MODEL_ID); experimental.setText(R.string.use_ai);
        experimental.setTextColor(INK);experimental.setTextSize(14);experimental.setEnabled(false);experimental.setSaveEnabled(false);
        entry.addView(experimental);
        entry.addView(text("Optional. Trained on small synthetic English examples; it can give false warnings. Message and address rules are always on.",12,MUTED,false));
        experimental.setOnCheckedChangeListener((v,enabled)->invalidateCheck());
        check=button("Check privately",true); check.setId(CHECK_ID); check.setEnabled(false);
        entry.addView(check,margin(14,0)); check.setOnClickListener(v->startCheck());
        cancel=button("Cancel check",false);cancel.setId(CANCEL_ID);cancel.setVisibility(View.GONE);
        entry.addView(cancel,margin(5,0));cancel.setOnClickListener(v->{invalidateCheck();status.setText(R.string.cancelled);});
        status=text("Preparing offline checks...",12,MUTED,false);status.setId(STATUS_ID);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);entry.addView(status,margin(10,0));

        results=card();results.setVisibility(View.GONE);page.addView(results,margin(16,0));
        outcome=text("",23,INK,true);outcome.setId(RESULT_ID);outcome.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);results.addView(outcome);
        evidence=vertical();results.addView(evidence);

        LinearLayout examples=card();page.addView(examples,margin(16,0));
        examples.addView(text("Try an example",16,INK,true));
        examples.addView(text("Made-up messages for exploring the app.",12,MUTED,false),margin(4,7));
        example(examples,"Fake KYC request","HDFC Bank: Your account will be blocked. Update KYC immediately at https://hdfc.bank.in.fake-site.com/verify");
        example(examples,"Normal OTP notice","Your OTP is 123456. Do not share your OTP with anyone.");
        example(examples,"Unknown short link","Can you check this? https://bit.ly/example");
        Button privacy=button("Privacy and limits",false);privacy.setId(PRIVACY_ID);page.addView(privacy,margin(12,0));privacy.setOnClickListener(v->showPrivacy());
        Button controls=button("Connections, history and updates",false);page.addView(controls,margin(8,0));controls.setOnClickListener(v->startActivity(new Intent(this,PrivacyActivity.class)));
        page.addView(text("No account. No internet permission. History is optional and off by default.\nEnglish MVP - this app cannot verify a sender or a live website.",12,MUTED,false),margin(8,0));
        input.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int f){}
            public void onTextChanged(CharSequence s,int a,int b,int c){invalidateCheck();}
            public void afterTextChanged(Editable e){}
        });
        page.setFocusableInTouchMode(true);page.requestFocus();
    }
    private void example(LinearLayout parent,String label,String message) {
        Button b=button(label,false);parent.addView(b);b.setOnClickListener(v->{partialInput=false;input.setText(message);status.setText(R.string.example_loaded);});
    }
    private void receive(Intent incoming) {
        if(incoming==null)return;
        try {
            String token=incoming.getStringExtra("preview_token");
            if(token!=null){incoming.removeExtra("preview_token");invalidateCheck();CheckResult preview=PreviewAlerts.take(token);input.setText("");freshPreview=true;
                if(preview!=null){showResult(preview);status.setText("Only a notification preview was checked. No full chat was read.");}
                else status.setText("Preview details expired or were cleared. Share the message to check it again.");return;}
            CharSequence shared=null;
            if(Intent.ACTION_SEND.equals(incoming.getAction()) && "text/plain".equals(incoming.getType()))shared=incoming.getCharSequenceExtra(Intent.EXTRA_TEXT);
            else if(Intent.ACTION_PROCESS_TEXT.equals(incoming.getAction()) && "text/plain".equals(incoming.getType()))shared=incoming.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            else if(Intent.ACTION_VIEW.equals(incoming.getAction()) && incoming.getData()!=null &&
                Set.of("http","https").contains(incoming.getData().getScheme()))shared=incoming.getData().toString();
            if(shared!=null){
                partialInput=shared.length()>ScamChecker.MAX_CHARACTERS;
                input.setText(shared.subSequence(0,Math.min(shared.length(),ScamChecker.MAX_CHARACTERS+1)));
                status.setText(partialInput?"Shared text is too long; only its first part can be checked.":"Shared content received. Review it, then tap Check.");
                incoming.removeExtra(Intent.EXTRA_TEXT);incoming.removeExtra(Intent.EXTRA_PROCESS_TEXT);
                incoming.setData(null);
            }
        }catch(RuntimeException e){status.setText(R.string.share_error);}
    }
    @Override public void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);receive(intent);}
    private void paste(){
        ClipboardManager clipboard=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(clipboard==null || !clipboard.hasPrimaryClip()){status.setText(R.string.clipboard_empty);return;}
        ClipData data=clipboard.getPrimaryClip();
        if(data==null || data.getItemCount()==0)return;
        CharSequence value=data.getItemAt(0).getText(); // Never dereference clipboard URIs.
        if(value==null){status.setText(R.string.clipboard_file);return;}
        partialInput=value.length()>ScamChecker.MAX_CHARACTERS;
        input.setText(value.subSequence(0,Math.min(value.length(),ScamChecker.MAX_CHARACTERS+1)));
        status.setText(R.string.pasted);
    }
    private void startCheck(){
        if(checker==null || input.getText().toString().isBlank())return;
        invalidateCheck();final long ticket=revision;final String content=input.getText().toString();
        final boolean useModel=experimental.isChecked(), partial=partialInput;
        busy=true;updateButtons();status.setText(R.string.checking);
        InputMethodManager keyboard=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(input.getWindowToken(),0);
        pending=worker.submit(()->{
            CheckResult result=null;try{result=EngineFactory.create(this).check(content,useModel,partial);}catch(Exception ignored){}
            final CheckResult completed=result;
            runOnUiThread(()->{
                if(destroyed || ticket!=revision)return;
                busy=false;pending=null;updateButtons();
                if(completed==null){status.setText(R.string.check_error);return;}
                showResult(completed);status.setText(completed.method().startsWith("rules +")?
                    "Checked locally with experimental AI and rules.":completed.method().contains("unavailable")?
                    "Model unavailable. Checked with rules only.":completed.method().contains("skipped")?
                    "Checked with rules. AI skipped for unsupported language.":"Checked locally with message and address rules.");
                if(new LocalSettings(this).history())worker.execute(()->{try{new EncryptedHistory(this).save(completed);}catch(Exception e){runOnUiThread(()->{if(!destroyed && ticket==revision)status.setText("Check finished, but the encrypted summary could not be saved.");});}});
            });
        });
    }
    private void invalidateCheck(){
        revision++;if(pending!=null){pending.cancel(true);pending=null;}busy=false;
        if(results!=null)results.setVisibility(View.GONE);
        if(evidence!=null)evidence.removeAllViews();if(outcome!=null)outcome.setText("");updateButtons();
        if(status!=null && checker!=null)status.setText(R.string.ready);
    }
    private void updateButtons(){if(check!=null){check.setEnabled(checker!=null && !busy && input.length()>0);check.setAlpha(check.isEnabled()?1f:0.45f);}if(cancel!=null)cancel.setVisibility(busy?View.VISIBLE:View.GONE);}
    private void showResult(CheckResult result){
        evidence.removeAllViews();results.setVisibility(View.VISIBLE);outcome.setText(result.level().label);
        int tone=result.level()==CheckResult.Level.STRONG_WARNING?0xffAD3932:result.level()==CheckResult.Level.NEEDS_CHECKING?0xff8A610D:GREEN;
        outcome.setTextColor(tone);
        evidence.addView(text(result.level()==CheckResult.Level.NO_WARNING_SIGNS?"The available checks found no strong concern. This does not guarantee safety.":"Pause and verify the request through an independently known channel.",14,MUTED,false),margin(8,12));
        if(!result.findings().isEmpty()){
            evidence.addView(text("Why this result",16,INK,true));
            for(CheckResult.Finding f:result.findings())evidence.addView(text("• "+f.explanation(),14,MUTED,false),margin(8,0));
        }
        for(CheckResult.Link link:result.links()){
            evidence.addView(text("Actual website address",12,MUTED,true),margin(14,3));
            TextView address=text(link.siteAddress().isEmpty()?"Could not identify reliably":link.siteAddress(),17,INK,true);
            address.setTextIsSelectable(true);evidence.addView(address);
        }
        evidence.addView(text("What to do next",16,INK,true),margin(16,3));
        for(String action:result.actions())evidence.addView(text(action,14,MUTED,false),margin(5,0));
        evidence.addView(text("Open the organisation's official app yourself. Links are never opened automatically.",13,GREEN,false),margin(12,0));
        String rawLink=input.getText().toString().trim();
        if(LinkGate.openable(rawLink)){
            Button open=button("Open link in another app",false);evidence.addView(open,margin(8,0));open.setOnClickListener(v->confirmHandoff(rawLink));
        }
        evidence.addView(text(result.method().startsWith("rules +")?"Experimental local AI ran. Its result is a suggestion, not proof of fraud.":result.method().contains("unavailable")?"Model unavailable; only rules ran.":result.method().contains("skipped")?"AI skipped: language coverage is limited.":"Message and address rules ran. Experimental AI is off.",12,MUTED,false),margin(14,0));
        scroll.post(()->{if(!destroyed)scroll.smoothScrollTo(0,results.getTop()-dp(16));});
    }
    private void confirmHandoff(String rawLink){
        AlertDialog confirm=new AlertDialog.Builder(this).setTitle("Continue to another app?").setMessage("ScamCheck has not inspected the live website. Another app may connect to the internet and receive the complete link. Your message text is not handed over. Only continue if you accept the risk.")
            .setNegativeButton("Stay here",null).setPositiveButton("Choose app",(dialog,which)->{
                Intent view=new Intent(Intent.ACTION_VIEW,Uri.parse(rawLink)).addCategory(Intent.CATEGORY_BROWSABLE);
                List<ResolveInfo> choices=getPackageManager().queryIntentActivities(view,0).stream()
                    .filter(info->info.activityInfo.exported && !info.activityInfo.packageName.equals(getPackageName())).limit(32).toList();
                if(choices.isEmpty()){status.setText("No other app can open this link. Use your usual browser manually.");return;}
                String[] labels=choices.stream().map(info->info.loadLabel(getPackageManager()).toString()).toArray(String[]::new);
                AlertDialog apps=new AlertDialog.Builder(this).setTitle("Open in").setItems(labels,(d,index)->{
                    ResolveInfo info=choices.get(index);Intent handoff=new Intent(view).setComponent(new ComponentName(info.activityInfo.packageName,info.activityInfo.name));
                    try{startActivity(handoff);}catch(ActivityNotFoundException|SecurityException e){status.setText("The selected app could not open the link.");}
                }).setNegativeButton("Cancel",null).create();apps.show();apps.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }).create();confirm.show();confirm.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
    private void showPrivacy(){
        new AlertDialog.Builder(this).setTitle("Private by design").setMessage(
            "Checks run on this phone. The app has no internet permission, account, ads or analytics. Original messages are never saved. Optional manual summaries are encrypted and excluded from backups. Clear removes the current input and result.\n\n"+
            "A pasted message also exists in your device clipboard, controlled by Android and the source app. This app reads it only when you tap Paste.\n\n"+
            "Offline checks cannot inspect a live page, follow a short link or prove who sent a message. The saved organisation list is small. English is the supported MVP language.\n\n"+
            "Experimental AI is off by default. Its synthetic held-out test flagged 6 of 15 genuine examples; these small development results are not real-world accuracy. AI alone can request review, never issue a strong warning.\n\n"+
            "Notification previews require separate consent and Android access. Only selected WhatsApp previews are checked, never chat history or SMS inboxes. Link routing depends on Android and the source app; share/paste remains available.\n\n"+
            "Version 0.3.0 · "+new UpdateStore(this).status())
            .setPositiveButton("Got it",null).show();
    }
    @Override protected void onStop(){super.onStop();if(busy)invalidateCheck();}
    @Override protected void onDestroy(){destroyed=true;revision++;if(pending!=null)pending.cancel(true);worker.shutdownNow();if(input!=null)input.setText("");super.onDestroy();}
    private LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);return l;}
    private LinearLayout card(){LinearLayout l=vertical();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(shape(Color.WHITE,18,0xffE2E8E0));return l;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(48));b.setTextColor(primary?Color.WHITE:GREEN);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?GREEN:0xffEEF4EE));return b;}
    private GradientDrawable shape(int fill,int radius,int border){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));d.setStroke(dp(1),border);return d;}
    private LinearLayout.LayoutParams margin(int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(top),0,dp(bottom));return p;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(0,0,dp(6),0);return p;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
