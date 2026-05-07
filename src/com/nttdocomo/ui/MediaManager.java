package com.nttdocomo.ui;

public class MediaManager {

    public static MediaSound getSound(String uri) {
        String path = normalizeResourcePath(uri);
        if (path.toLowerCase().endsWith(".mld")) {
            return new MldMediaSound(path);
        }
        return new BasicMediaSound(path);
    }

    public static MediaImage getImage(String uri) {
        String path = normalizeResourcePath(uri);
        return new BasicMediaImage(path);
    }

    private static final class BasicMediaSound implements MediaSound {
        private final String resourcePath;

        BasicMediaSound(String path) {
            resourcePath = path;
        }

        public void use() {
            System.out.println("[DoJa] Unsupported sound resource: " + resourcePath);
        }
    }

    private static final class BasicMediaImage implements MediaImage {
        private final String resourcePath;
        private Image dojaImage;

        BasicMediaImage(String path) {
            resourcePath = path;
        }

        public void use() {
            String name = resourcePath;
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            String[] paths = { "/" + name, "/res/" + name };
            for (int i = 0; i < paths.length; i++) {
                try {
                    javax.microedition.lcdui.Image midpImg =
                        javax.microedition.lcdui.Image.createImage(paths[i]);
                    dojaImage = new Image(midpImg);
                    return;
                } catch (Exception e) {
                }
            }
            System.out.println("[DoJa] MediaImage.use() failed: " + resourcePath);
        }

        public Image getImage() {
            return dojaImage;
        }
    }

    private static String normalizeResourcePath(String uri) {
        String path = uri == null ? "" : uri;
        if (path.startsWith("resource:///")) {
            path = path.substring("resource:///".length());
        }
        return path;
    }
}

