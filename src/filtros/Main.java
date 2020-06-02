package filtros;

import org.opencv.core.Core;

public class Main {

    public static void main(String[] args) {
        // Load the native library.
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new SobelDemoRun().run(args);
//        new CannyDetectorDemo(args);
//        new PrewittDemo(args);
    }
}
