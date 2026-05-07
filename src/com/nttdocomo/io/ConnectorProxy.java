package com.nttdocomo.io;

import java.io.IOException;
import java.io.InputStream;

public final class ConnectorProxy {
    private static final String RESOURCE_PREFIX = "resource:///";

    private ConnectorProxy() {
    }

    public static InputStream openInputStream(String uri) throws IOException {
        String name = uri == null ? "" : uri;
        String[] paths;
        int i;

        if (name.startsWith(RESOURCE_PREFIX)) {
            name = name.substring(RESOURCE_PREFIX.length());
        }
        if (name.startsWith("/")) {
            name = name.substring(1);
        }

        paths = new String[] { "/" + name, "/res/" + name };
        for (i = 0; i < paths.length; i++) {
            InputStream input = ConnectorProxy.class.getResourceAsStream(paths[i]);
            if (input != null) {
                return input;
            }
        }

        throw new IOException("Resource not found: " + uri);
    }
}
