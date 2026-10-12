package in.scamcheck.app;
import android.content.*;
import java.util.*;

final class LocalSettings {
    static final String PREFS="privacy_controls";
    static final String WHATSAPP="com.whatsapp", BUSINESS="com.whatsapp.w4b";
    private final SharedPreferences prefs;
    LocalSettings(Context context){prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    boolean monitoring(){return prefs.getBoolean("monitoring",false) && prefs.getBoolean("consent",false);}
    boolean history(){return prefs.getBoolean("history",false);}
    Set<String> sources(){return new HashSet<>(prefs.getStringSet("sources",Set.of(WHATSAPP,BUSINESS)));}
    void monitoring(boolean enabled){prefs.edit().putBoolean("consent",enabled).putBoolean("monitoring",enabled).commit();}
    void history(boolean enabled){prefs.edit().putBoolean("history",enabled).commit();}
    void sources(Set<String> selected){prefs.edit().putStringSet("sources",new HashSet<>(selected)).commit();}
}
