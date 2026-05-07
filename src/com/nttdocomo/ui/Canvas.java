package com.nttdocomo.ui;

import com.nttdocomo.ui.graphics3d.Graphics3DImpl;

// DoJa Canvas bridge 鈥?extends Frame (鈫扢IDP Canvas), provides DoJa Graphics hierarchy
public abstract class Canvas extends Frame implements Runnable {

    private Graphics3DImpl dojaGraphics;
    private volatile int keyState;
    private final String[] softLabels = new String[2];

    protected Canvas() {
        setFullScreenMode(true);
    }

    // DoJa: returns Graphics3D (castable to Graphics and Graphics2)
    public Graphics getGraphics() {
        if (dojaGraphics == null) {
            dojaGraphics = new Graphics3DImpl();
            dojaGraphics.parentCanvas = this;
        }
        dojaGraphics.lock();
        return dojaGraphics;
    }

    // DoJa: get current key state bitmask
    public int getKeypadState() {
        return keyState;
    }

    // DoJa: set soft key labels (stub 鈥?MIDP has no equivalent)
    public void setSoftLabel(int index, String label) {
        if (index >= 0 && index < softLabels.length) {
            softLabels[index] = label;
        }
    }

    // MIDP Canvas paint 鈥?flush DoJa back buffer to screen
    protected void paint(javax.microedition.lcdui.Graphics g) {
        if (dojaGraphics != null && dojaGraphics.getDisplayBuffer() != null) {
            g.drawImage(dojaGraphics.getDisplayBuffer(), 0, 0,
                javax.microedition.lcdui.Graphics.TOP | javax.microedition.lcdui.Graphics.LEFT);
        }
    }

    // Map MIDP key events to DoJa key bitmask
    protected void keyPressed(int keyCode) {
        keyState |= mapKey(keyCode);
    }

    protected void keyReleased(int keyCode) {
        keyState &= ~mapKey(keyCode);
    }

    // DoJa key constants mapping
    private int mapKey(int keyCode) {
        int action = 0;
        try { action = getGameAction(keyCode); } catch (Exception e) {}
        switch (action) {
            case UP:    return 0x20000;
            case DOWN:  return 0x80000;
            case LEFT:  return 0x10000;
            case RIGHT: return 0x40000;
            case FIRE:  return 0x100000;
        }
        // Soft keys
        if (keyCode == -6 || keyCode == -21) return 0x200000;
        if (keyCode == -7 || keyCode == -22) return 0x400000;
        return 0;
    }
}

