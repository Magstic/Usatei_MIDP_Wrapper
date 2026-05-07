import javax.microedition.midlet.MIDlet;

import com.nttdocomo.ui.IApplication;

public class OriginalMainMidlet extends MIDlet implements Runnable {

    private static final String APP_CLASS_RESOURCE = "/META-INF/doja-wrapper-app.txt";

    private IApplication app;
    private boolean started;

    public void startApp() {
        if (started) {
            return;
        }
        started = true;
        IApplication.setMidlet(this);
        try {
            app = (IApplication)Class.forName(readAppClassName()).newInstance();
            IApplication.setCurrentApp(app);
            new Thread(this).start();
        } catch (Exception e) {
            e.printStackTrace();
            notifyDestroyed();
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void run() {
        app.start();
    }

    private String readAppClassName() throws java.io.IOException {
        java.io.InputStream input = OriginalMainMidlet.class.getResourceAsStream(APP_CLASS_RESOURCE);
        java.io.ByteArrayOutputStream output;
        byte[] buffer;
        int read;

        if (input == null) {
            throw new java.io.IOException("Missing " + APP_CLASS_RESOURCE);
        }

        output = new java.io.ByteArrayOutputStream();
        buffer = new byte[64];
        try {
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            }
        } finally {
            input.close();
        }
        return output.toString().trim();
    }
}

