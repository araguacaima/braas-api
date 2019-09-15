package com.araguacaima.braas.core;

import com.araguacaima.commons.utils.ClassLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Commons {
    private static ClassLoaderUtils classLoaderUtils = new ClassLoaderUtils(null, null);


    public static File findFile(String filePath) throws IOException {
        return findFile(filePath, null);
    }

    public static File findFile(String filePath, ClassLoader cl) throws IOException {
        if (cl == null) {
            cl = Commons.class.getClassLoader();
        }
        InputStream inputstream = cl.getResourceAsStream(filePath);
        URL resource = cl.getResource(filePath);
        if (resource != null) {
            return new File(resource.getPath());
        }
        if (inputstream == null) {
            inputstream = classLoaderUtils.getClassLoader().getResourceAsStream(filePath);
            resource = classLoaderUtils.getClassLoader().getResource(filePath);
            if (resource != null) {
                return new File(resource.getPath());
            }
            if (inputstream == null) {
                inputstream = classLoaderUtils.getClassLoader().getParent().getResourceAsStream(filePath);
                resource = classLoaderUtils.getClassLoader().getParent().getResource(filePath);
                if (resource != null) {
                    return new File(resource.getPath());
                }
                if (inputstream == null) {
                    inputstream = ClassLoader.getSystemClassLoader().getResourceAsStream(filePath);
                    resource = ClassLoader.getSystemClassLoader().getResource(filePath);
                    if (resource != null) {
                        return new File(resource.getPath());
                    }
                    if (inputstream == null) {
                        try {
                            inputstream = new java.io.FileInputStream((new File(filePath)));
                            resource = (new File(filePath)).toURI().toURL();
                            filePath = resource.getPath();
                        } catch (Throwable t) {
                            if (!filePath.startsWith(".")) {
                                String prefix = ".";
                                if (!filePath.startsWith("/") && !filePath.startsWith("\\\\")) {
                                    prefix = prefix + "/";
                                }
                                return findFile(prefix + filePath, cl);
                            }
                        }
                    }
                }
            }
        }
        inputstream.close();
        return new File(filePath);
    }
}
