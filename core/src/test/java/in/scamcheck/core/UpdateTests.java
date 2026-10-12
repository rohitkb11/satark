package in.scamcheck.core;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.*;

public final class UpdateTests {
    private static int passed;private static KeyPair keys;private static final LocalDate TODAY=LocalDate.of(2026,10,12);
    public static void main(String[] args)throws Exception{
        KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA");gen.initialize(2048);keys=gen.generateKeyPair();
        byte[] brands;try(InputStream in=EngineResources.BUNDLED.open("brands.tsv")){brands=in.readAllBytes();}
        byte[] valid=packet(1,TODAY,TODAY.plusDays(30),Map.of("brands.tsv",brands));
        SignedUpdate.Bundle bundle=SignedUpdate.verify(valid,keys.getPublic(),0,TODAY);
        require(bundle.version()==1,"Valid signed data accepted");
        require(new AssistedChecker(bundle).modelAvailable(),"Omitted model uses bundled model");
        rejects(valid,keys.getPublic(),1,TODAY,"Replay rejected");
        byte[] changed=valid.clone();changed[changed.length-1]^=1;rejects(changed,keys.getPublic(),0,TODAY,"Tamper rejected");
        rejects(valid,gen.generateKeyPair().getPublic(),0,TODAY,"Foreign publisher rejected");
        rejects(packet(1,TODAY.minusDays(10),TODAY.minusDays(1),Map.of("brands.tsv",brands)),keys.getPublic(),0,TODAY,"Expired data rejected");
        rejects(packet(1,TODAY.plusDays(1),TODAY.plusDays(30),Map.of("brands.tsv",brands)),keys.getPublic(),0,TODAY,"Future data rejected");
        rejects(packet(1,TODAY,TODAY.plusDays(400),Map.of("brands.tsv",brands)),keys.getPublic(),0,TODAY,"Excess validity rejected");
        rejects(packet(1,TODAY,TODAY.plusDays(30),Map.of("../brands.tsv",brands)),keys.getPublic(),0,TODAY,"Traversal rejected");
        rejects(packet(1,TODAY,TODAY.plusDays(30),Map.of("code.apk",brands)),keys.getPublic(),0,TODAY,"Executable payload rejected");
        rejects(packet(1,TODAY,TODAY.plusDays(30),Map.of("message_model.tsv","bad".getBytes(StandardCharsets.UTF_8))),keys.getPublic(),0,TODAY,"Broken model rejected");
        rejects(packet(1,TODAY,TODAY.plusDays(30),Map.of("message_model.tsv",new byte[300*1024])),keys.getPublic(),0,TODAY,"Inflation limit enforced");
        String broad="bad\tBad\tbad\tcom\thttps://example.org\t2026-10-12\n";
        rejects(packet(1,TODAY,TODAY.plusDays(30),Map.of("brands.tsv",broad.getBytes(StandardCharsets.UTF_8))),keys.getPublic(),0,TODAY,"Public suffix cannot become a trusted root");
        byte[] trailing=Arrays.copyOf(valid,valid.length+1);rejects(trailing,keys.getPublic(),0,TODAY,"Trailing bytes rejected");
        rejects(Arrays.copyOf(valid,20),keys.getPublic(),0,TODAY,"Truncated envelope rejected");
        rejects(new byte[SignedUpdate.MAX_BYTES+1],keys.getPublic(),0,TODAY,"Envelope bound enforced");
        byte[] external=bundle.files().get("brands.tsv");external[0]^=1;
        try(InputStream in=bundle.open("brands.tsv")){require(Arrays.equals(brands,in.readAllBytes()),"Verified data cannot be mutated through getter");}
        System.out.println("PASS: "+passed+" signed-update safety checks.");
    }
    private static byte[] packet(long version,LocalDate created,LocalDate expires,Map<String,byte[]> files)throws Exception{
        String manifest="version="+version+"\ncreated="+created+"\nexpires="+expires+"\ndescription=Test data\nsource=Authored fixtures\nlicense=Project test data\n";
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){for(var entry:files.entrySet()){zip.putNextEntry(new ZipEntry(entry.getKey()));zip.write(entry.getValue());zip.closeEntry();}}
        return SignedUpdate.pack(manifest.getBytes(StandardCharsets.UTF_8),out.toByteArray(),keys.getPrivate());
    }
    private static void rejects(byte[] data,PublicKey key,long high,LocalDate today,String label)throws Exception{boolean rejected=false;try{SignedUpdate.verify(data,key,high,today);}catch(Exception e){rejected=true;}require(rejected,label);}
    private static void require(boolean value,String label){if(!value)throw new AssertionError(label);passed++;}
}
