package org.sikuli.recorder;

import java.awt.image.BufferedImage;
import java.io.File;

public interface SaveFile {

    String saveTo(BufferedImage bimg, File path);
    File createDirectory(String prefix);
}
