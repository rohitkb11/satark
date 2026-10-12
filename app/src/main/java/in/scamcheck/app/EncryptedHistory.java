package in.scamcheck.app;

import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import in.scamcheck.core.CheckResult;
import in.scamcheck.core.SignedUpdate;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** AES-GCM with a device-bound Android Keystore key. Only manual result summaries, never input text. */
final class EncryptedHistory {
    private static final String ALIAS="scamcheck_history_v1";
    private static final Object LOCK=new Object();
    private static final byte[] AAD="ScamCheck history v1".getBytes(StandardCharsets.UTF_8);
    private final AtomicFile file;
    private final Context context;
    EncryptedHistory(Context context){this.context=context.getApplicationContext();file=new AtomicFile(new File(context.getNoBackupFilesDir(),"history.enc"));}
    private SecretKey key()throws Exception{
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(ALIAS)){
            KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());generator.generateKey();
        }
        return (SecretKey)store.getKey(ALIAS,null);
    }
    JSONArray entries()throws Exception{synchronized(LOCK){return readEntries();}}
    private JSONArray readEntries()throws Exception{
        if(!file.getBaseFile().exists() && !new File(file.getBaseFile().getPath()+".bak").exists())return new JSONArray();byte[] bytes;
        try(InputStream in=file.openRead()){bytes=SignedUpdate.readBounded(in,256*1024);}
        if(bytes.length<32 || bytes[0]!=1)throw new IOException("History cannot be read safely");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOfRange(bytes,1,13)));cipher.updateAAD(AAD);
        return new JSONArray(new String(cipher.doFinal(Arrays.copyOfRange(bytes,13,bytes.length)),StandardCharsets.UTF_8));
    }
    void save(CheckResult result)throws Exception{synchronized(LOCK){saveLocked(result);}}
    private void saveLocked(CheckResult result)throws Exception{
        if(!new LocalSettings(context).history())return;
        JSONArray entries=entries();JSONObject summary=new JSONObject();summary.put("id",UUID.randomUUID().toString());summary.put("time",System.currentTimeMillis());
        summary.put("level",result.level().label);summary.put("method",result.method());JSONArray domains=new JSONArray(),codes=new JSONArray();
        for(CheckResult.Link link:result.links())if(!link.siteAddress().isEmpty())domains.put(link.siteAddress());
        for(CheckResult.Finding finding:result.findings())codes.put(finding.code());summary.put("domains",domains);summary.put("codes",codes);
        JSONArray kept=new JSONArray();kept.put(summary);for(int i=0;i<Math.min(entries.length(),49);i++)kept.put(entries.get(i));write(kept);
    }
    void delete(String id)throws Exception{synchronized(LOCK){JSONArray entries=entries(),kept=new JSONArray();for(int i=0;i<entries.length();i++)if(!entries.getJSONObject(i).getString("id").equals(id))kept.put(entries.get(i));write(kept);}}
    private void write(JSONArray entries)throws Exception{
        byte[] plaintext=entries.toString().getBytes(StandardCharsets.UTF_8);
        while(plaintext.length>128*1024 && entries.length()>1){entries.remove(entries.length()-1);plaintext=entries.toString().getBytes(StandardCharsets.UTF_8);}
        if(plaintext.length>128*1024)throw new IOException("Summary is too large to save");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());cipher.updateAAD(AAD);
        if(cipher.getIV().length!=12)throw new IOException("Unsupported encryption nonce");
        byte[] encrypted=cipher.doFinal(plaintext);
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(1);out.write(cipher.getIV());out.write(encrypted);
        FileOutputStream stream=null;
        try{stream=file.startWrite();stream.write(out.toByteArray());file.finishWrite(stream);}catch(IOException e){if(stream!=null)file.failWrite(stream);throw e;}
    }
    void clear()throws Exception{synchronized(LOCK){clearLocked();}}
    private void clearLocked()throws Exception{
        file.delete();KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(store.containsAlias(ALIAS))store.deleteEntry(ALIAS);
        if(file.getBaseFile().exists())throw new IOException("History removal failed");
    }
}
