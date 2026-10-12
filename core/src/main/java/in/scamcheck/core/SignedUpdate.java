package in.scamcheck.core;

import java.io.*;
import java.nio.charset.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.*;

/** Bounded, authenticated data-only update envelope. Fail closed before activation. */
public final class SignedUpdate {
    public static final int MAGIC=0x53435531, MAX_BYTES=1024*1024;
    public record Bundle(long version, LocalDate created, LocalDate expires, String description,
                         String source, String license, Map<String,byte[]> files) implements EngineResources {
        public Bundle { Map<String,byte[]> copy=new HashMap<>();files.forEach((k,v)->copy.put(k,v.clone()));files=Collections.unmodifiableMap(copy); }
        @Override public Map<String,byte[]> files(){Map<String,byte[]> copy=new HashMap<>();files.forEach((k,v)->copy.put(k,v.clone()));return Collections.unmodifiableMap(copy);}
        public InputStream open(String name)throws IOException { byte[] value=files.get(name);return value==null?EngineResources.BUNDLED.open(name):new ByteArrayInputStream(value); }
    }
    public static PublicKey bundledKey()throws Exception{
        try(InputStream in=EngineResources.BUNDLED.open("update_public_key.der")){
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(readBounded(in,1024)));
        }
    }
    public static Bundle verify(byte[] envelope,PublicKey key,long highWater,LocalDate today)throws Exception{
        if(envelope.length>MAX_BYTES)throw new IOException("Update is too large");
        DataInputStream input=new DataInputStream(new ByteArrayInputStream(envelope));
        if(input.readInt()!=MAGIC)throw new IOException("Unsupported update format");
        byte[] metadata=field(input,8192),signature=field(input,1024),payload=field(input,MAX_BYTES-10000);
        if(input.read()!=-1)throw new IOException("Unexpected update data");
        Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(key);verifier.update(signedBytes(metadata,payload));
        if(!verifier.verify(signature))throw new IOException("Update signature is not trusted");
        Map<String,String> manifest=new HashMap<>();
        for(String line:utf8(metadata).split("\n")){
            int split=line.indexOf('=');if(split<=0 || manifest.put(line.substring(0,split),line.substring(split+1))!=null)throw new IOException("Invalid update metadata");
        }
        if(!manifest.keySet().equals(Set.of("version","created","expires","description","source","license")))throw new IOException("Incomplete update metadata");
        long version=Long.parseLong(manifest.get("version"));
        LocalDate created=LocalDate.parse(manifest.get("created")),expires=LocalDate.parse(manifest.get("expires"));
        if(version<=highWater || version<=0)throw new IOException("Update version must be newer than every previously imported version");
        if(created.isAfter(today) || expires.isBefore(today) || expires.isBefore(created) || expires.isAfter(created.plusDays(365)))throw new IOException("Update is expired or its dates are invalid");
        for(String value:manifest.values())if(value.isBlank() || value.length()>1024 || value.indexOf('\r')>=0 || value.chars().anyMatch(c->c<32))throw new IOException("Invalid update description");
        Map<String,byte[]> files=new HashMap<>();
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(payload))){
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){
                String name=entry.getName();
                if(entry.isDirectory() || !Set.of("brands.tsv","message_model.tsv").contains(name) || files.containsKey(name))throw new IOException("Unexpected or repeated update file");
                files.put(name,readBounded(zip,name.equals("brands.tsv")?128*1024:256*1024));
            }
        }
        if(files.isEmpty())throw new IOException("Update contains no supported data");
        if(files.containsKey("message_model.tsv"))new LocalTextModel(new ByteArrayInputStream(files.get("message_model.tsv")));
        if(files.containsKey("brands.tsv")){
            String text=utf8(files.get("brands.tsv"));
            BrandRegistry registry=new BrandRegistry(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
            PublicSuffixes psl=new PublicSuffixes();
            if(registry.brands().isEmpty() || registry.brands().size()>100)throw new IOException("Invalid website list size");
            Set<String> ids=new HashSet<>(),roots=new HashSet<>();
            for(BrandRegistry.Brand brand:registry.brands()){
                if(!ids.add(brand.id()) || !brand.id().matches("[a-z0-9-]{3,40}") || brand.name().length()>80 || brand.aliases().size()>10 || brand.roots().size()>20)throw new IOException("Invalid organisation entry");
                if(!brand.source().startsWith("https://") || LocalDate.parse(brand.checkedDate()).isAfter(today))throw new IOException("Website entry needs a source and valid check date");
                for(String alias:brand.aliases())if(alias.length()<3 || alias.length()>80)throw new IOException("Invalid organisation alias");
                for(String root:brand.roots())if(!root.matches("[a-z0-9.-]{3,253}") || !roots.add(root) || !psl.knownSuffix(root) || !psl.registrableDomain(root).equals(root))throw new IOException("Website entry must use a unique registrable domain");
            }
        }
        return new Bundle(version,created,expires,manifest.get("description"),manifest.get("source"),manifest.get("license"),files);
    }
    public static byte[] signedBytes(byte[] manifest,byte[] payload)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(out);
        data.writeUTF("ScamCheck data update v1");data.writeInt(manifest.length);data.write(manifest);data.writeInt(payload.length);data.write(payload);return out.toByteArray();
    }
    public static byte[] pack(byte[] manifest,byte[] payload,PrivateKey key)throws Exception{
        Signature signer=Signature.getInstance("SHA256withRSA");signer.initSign(key);signer.update(signedBytes(manifest,payload));
        byte[] signature=signer.sign();ByteArrayOutputStream out=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(out);
        data.writeInt(MAGIC);for(byte[] part:new byte[][]{manifest,signature,payload}){data.writeInt(part.length);data.write(part);}return out.toByteArray();
    }
    public static byte[] readBounded(InputStream in,int limit)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(n==0)continue;if(out.size()+n>limit)throw new IOException("Data exceeds its allowed size");out.write(buffer,0,n);}return out.toByteArray();
    }
    private static byte[] field(DataInputStream in,int limit)throws IOException{int length=in.readInt();if(length<=0 || length>limit)throw new IOException("Invalid update field size");byte[] data=new byte[length];in.readFully(data);return data;}
    private static String utf8(byte[] data)throws IOException{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(data)).toString();}
}
