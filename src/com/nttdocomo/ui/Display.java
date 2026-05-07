package com.nttdocomo.ui;

public class Display {

    public static void setCurrent(Frame frame) {
        IApplication app = IApplication.getCurrentApp();
        if (app != null) {
            javax.microedition.midlet.MIDlet midlet = getMidlet();
            if (midlet != null) {
                javax.microedition.lcdui.Display.getDisplay(midlet).setCurrent(frame);
            }
        }
    }

    private static javax.microedition.midlet.MIDlet getMidlet() {
        return DisplayMidletHolder.midlet;
    }

    static void setMidlet(javax.microedition.midlet.MIDlet midlet) {
        DisplayMidletHolder.midlet = midlet;
    }

    private static final class DisplayMidletHolder {
        static javax.microedition.midlet.MIDlet midlet;
    }
}

