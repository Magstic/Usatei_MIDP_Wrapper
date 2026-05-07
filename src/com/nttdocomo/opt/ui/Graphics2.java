package com.nttdocomo.opt.ui;

import com.nttdocomo.ui.Graphics;
import com.nttdocomo.ui.Image;

// DoJa Graphics2 bridge 鈥?extends Graphics with screen capture
public class Graphics2 extends Graphics {

    public Graphics2() {
        super();
    }

    // Capture screen region as DoJa Image
    public Image getImage(int x, int y, int w, int h) {
        if (backBuffer == null) return null;
        int[] rgb = new int[w * h];
        backBuffer.getRGB(rgb, 0, w, x, y, w, h);
        javax.microedition.lcdui.Image captured =
            javax.microedition.lcdui.Image.createRGBImage(rgb, w, h, true);
        return new Image(captured);
    }
}

