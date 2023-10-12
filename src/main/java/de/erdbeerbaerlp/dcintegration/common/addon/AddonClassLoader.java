package de.erdbeerbaerlp.dcintegration.common.addon;

import java.net.URL;
import java.net.URLClassLoader;

final class AddonClassLoader extends URLClassLoader {

    static {
        registerAsParallelCapable();
    }
    public AddonClassLoader(ClassLoader p) {
        super(new URL[0], p);
    }
    void add(URL url) {
        addURL(url);
    }

}