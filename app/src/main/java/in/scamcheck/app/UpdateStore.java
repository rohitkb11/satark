package in.scamcheck.app;
import android.content.Context;
import android.util.AtomicFile;
import in.scamcheck.core.*;
import org.json.JSONObject;
import java.io.*;
import java.security.*;
import java.time.LocalDate;
import java.util.*;

/** Immutable authenticated bundles + one atomic pointer commit. Failed imports leave current data intact. */
final class UpdateStore {
    private final File directory;
    private static final Object LOCK=new Object();
    private final AtomicFile pointer;
    private record State(String current,String previous,long highWater){}
    UpdateStore(Context context){directory=new File(context.getNoBackupFilesDir(),"updates");directory.mkdirs();pointer=new AtomicFile(new File(directory,"state.json"));}
    private State state()throws Exception{
        if(!pointer.getBaseFile().exists() && !new File(pointer.getBaseFile()+".bak").exists())return new State("","",0);
        try(InputStream in=pointer.openRead()){
            JSONObject data=new JSONObject(new String(SignedUpdate.readBounded(in,4096),java.nio.charset.StandardCharsets.UTF_8));
            String current=data.getString("current"),previous=data.getString("previous");long high=data.getLong("highWater");
            if(high<0 || (!current.isEmpty() && !current.matches("[a-f0-9]{64}")) || (!previous.isEmpty() && !previous.matches("[a-f0-9]{64}")))throw new IOException("Update state is invalid");
            return new State(current,previous,high);
        }
    }
    SignedUpdate.Bundle current()throws Exception{synchronized(LOCK){return currentLocked();}}
    private SignedUpdate.Bundle currentLocked()throws Exception{
        State state=state();return state.current.isEmpty()?null:read(state.current);
    }
    private SignedUpdate.Bundle read(String id)throws Exception{
        byte[] bytes;
        try(InputStream in=new FileInputStream(new File(directory,id+".scu"))){bytes=SignedUpdate.readBounded(in,SignedUpdate.MAX_BYTES);}
        if(!hash(bytes).equals(id))throw new IOException("Stored update integrity check failed");
        return SignedUpdate.verify(bytes,SignedUpdate.bundledKey(),0,LocalDate.now());
    }
    SignedUpdate.Bundle importUpdate(InputStream input)throws Exception{synchronized(LOCK){return importLocked(input);}}
    private SignedUpdate.Bundle importLocked(InputStream input)throws Exception{
        byte[] bytes=SignedUpdate.readBounded(input,SignedUpdate.MAX_BYTES);State old=state();
        SignedUpdate.Bundle bundle=SignedUpdate.verify(bytes,SignedUpdate.bundledKey(),old.highWater,LocalDate.now());
        // Verify resource initialization before changing any active pointer.
        new AssistedChecker(bundle);
        String id=hash(bytes);AtomicFile file=new AtomicFile(new File(directory,id+".scu"));write(file,bytes);
        commit(new State(id,old.current,bundle.version()));return bundle;
    }
    SignedUpdate.Bundle rollback()throws Exception{synchronized(LOCK){return rollbackLocked();}}
    private SignedUpdate.Bundle rollbackLocked()throws Exception{
        State state=state();if(state.previous.isEmpty())throw new IOException("No previous update is available");
        SignedUpdate.Bundle bundle=read(state.previous);new AssistedChecker(bundle);
        commit(new State(state.previous,"",state.highWater));return bundle;
    }
    String status(){try{SignedUpdate.Bundle bundle=current();return bundle==null?"Bundled website and model data":("Signed data v"+bundle.version()+" · expires "+bundle.expires());}catch(Exception e){return "Saved update unavailable; bundled data is used";}}
    String activeId(){synchronized(LOCK){try{return state().current;}catch(Exception e){return "unavailable";}}}
    private void commit(State state)throws Exception{
        JSONObject data=new JSONObject();data.put("current",state.current);data.put("previous",state.previous);data.put("highWater",state.highWater);
        write(pointer,data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Only app-owned, immutable update files with a strict generated name are pruned.
        File[] files=directory.listFiles();if(files!=null)for(File file:files){String name=file.getName();
            if(name.matches("[a-f0-9]{64}\\.scu") && !name.equals(state.current+".scu") && !name.equals(state.previous+".scu"))file.delete();
        }
    }
    private static void write(AtomicFile file,byte[] bytes)throws IOException{
        FileOutputStream stream=null;try{stream=file.startWrite();stream.write(bytes);file.finishWrite(stream);}catch(IOException e){if(stream!=null)file.failWrite(stream);throw e;}
    }
    private static String hash(byte[] bytes)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder();for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b & 255));return out.toString();}
}
