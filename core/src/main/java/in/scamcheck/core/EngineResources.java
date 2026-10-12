package in.scamcheck.core;
import java.io.*;

/** Only data resources are replaceable. No downloaded code is executed. */
public interface EngineResources {
    InputStream open(String name) throws IOException;
    EngineResources BUNDLED = name -> {
        InputStream input = EngineResources.class.getResourceAsStream("/" + name);
        if (input == null) throw new IOException("Required local resource is missing");
        return input;
    };
}
