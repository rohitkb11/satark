package in.scamcheck.app;
import java.net.URI;

/** Handoff only simple ASCII HTTP(S) addresses. Never resolve or fetch during checking. */
final class LinkGate {
    static boolean openable(String value){
        try{
            if(value.length()>8192 || !value.chars().allMatch(c->c>=33 && c<127) || value.contains("\\"))return false;
            URI uri=new URI(value);String host=uri.getHost();
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) &&
                host!=null && host.contains(".") && !host.contains("xn--") && !host.matches("[0-9.]+") &&
                uri.getRawUserInfo()==null && !uri.getRawAuthority().contains("%") && (uri.getPort()==-1 || uri.getPort()<=65535);
        }catch(Exception e){return false;}
    }
}
