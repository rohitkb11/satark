package in.scamcheck.app;
import android.content.Context;
import in.scamcheck.core.*;

final class EngineFactory {
    static AssistedChecker create(Context context)throws Exception{
        SignedUpdate.Bundle bundle=null;
        try{bundle=new UpdateStore(context).current();}catch(Exception ignored){ /* Failed/expired update falls back to built-in data. */ }
        return bundle==null?new AssistedChecker():new AssistedChecker(bundle);
    }
}
