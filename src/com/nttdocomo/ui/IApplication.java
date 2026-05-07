package com.nttdocomo.ui;

import javax.microedition.midlet.MIDlet;

public class IApplication {

    private static IApplication instance;
    private static MIDlet midlet;

    public static IApplication getCurrentApp() {
        return instance;
    }

    public static void setCurrentApp(IApplication app) {
        instance = app;
    }

    public static void setMidlet(MIDlet appMidlet) {
        midlet = appMidlet;
        Display.setMidlet(appMidlet);
    }

    public void start() {
    }

    public void terminate() {
        if (midlet != null) {
            midlet.notifyDestroyed();
        }
    }
}

