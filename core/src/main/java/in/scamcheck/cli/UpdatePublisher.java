package in.scamcheck.cli;
import in.scamcheck.core.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.LocalDate;
import java.io.*;
import java.util.zip.*;

/** Local development publisher. Private key stays in ignored .tools, never the APK. */
public final class UpdatePublisher {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),privatePath=root.resolve(".tools/publisher/private.pk8"),publicPath=root.resolve("core/src/main/resources/update_public_key.der");
        if(!Files.exists(privatePath)){
            if(Files.exists(publicPath))throw new IOException("Publisher key is missing; refusing to silently replace the trust anchor");
            Files.createDirectories(privatePath.getParent());KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair keys=generator.generateKeyPair();
            Files.write(privatePath,keys.getPrivate().getEncoded());Files.write(publicPath,keys.getPublic().getEncoded());
        }
        PrivateKey key=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Files.readAllBytes(privatePath)));
        long version=Long.parseLong(args[1]);LocalDate today=LocalDate.now();
        String manifest="version="+version+"\ncreated="+today+"\nexpires="+today.plusDays(90)+"\ndescription=Prototype website and model data update\nsource=ScamCheck authored development data and primary website sources\nlicense=Project-authored model; website facts only; no third-party threat feed\n";
        ByteArrayOutputStream payload=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(payload)){
            for(String name:new String[]{"brands.tsv","message_model.tsv"}){zip.putNextEntry(new ZipEntry(name));zip.write(Files.readAllBytes(root.resolve("core/src/main/resources/"+name)));zip.closeEntry();}
        }
        byte[] packet=SignedUpdate.pack(manifest.getBytes(StandardCharsets.UTF_8),payload.toByteArray(),key);
        Path dest=root.resolve("output/ScamCheck-update-v"+version+".scu");Files.createDirectories(dest.getParent());Files.write(dest,packet);
        System.out.println("Signed development update written. Publisher private key was not included.");
    }
}
