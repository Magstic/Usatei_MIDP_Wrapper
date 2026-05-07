import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public final class DoJaAppClassFinder {
    private static final String APP_SUPER = "com/nttdocomo/ui/IApplication";

    public static void main(String[] args) throws IOException {
        String found;
        if (args.length != 1) {
            throw new IOException("Usage: DoJaAppClassFinder <class-dir>");
        }
        found = find(new File(args[0]));
        if (found == null) {
            throw new IOException("Cannot find class extending " + APP_SUPER);
        }
        System.out.println(found);
    }

    private static String find(File root) throws IOException {
        ArrayList files = new ArrayList();
        int i;

        collectClasses(root, files);
        for (i = 0; i < files.size(); i++) {
            File file = (File) files.get(i);
            if (APP_SUPER.equals(readSuperClassName(file))) {
                return className(root, file);
            }
        }
        return null;
    }

    private static void collectClasses(File root, ArrayList output) {
        File[] files = root.listFiles();
        int i;

        if (files == null) {
            return;
        }
        for (i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                collectClasses(file, output);
            } else if (file.getName().endsWith(".class")) {
                output.add(file);
            }
        }
    }

    private static String readSuperClassName(File file) throws IOException {
        byte[] data = readAll(file);
        int offset;
        Object[] constants;
        int thisClass;
        int superClass;

        if (readU4(data, 0) != 0xCAFEBABE) {
            throw new IOException("Invalid class file: " + file);
        }

        offset = 8;
        constants = new Object[readU2(data, offset)];
        offset += 2;
        offset = readConstantPool(data, offset, constants);
        offset += 2;
        thisClass = readU2(data, offset);
        offset += 2;
        superClass = readU2(data, offset);

        if (thisClass == 0 || superClass == 0) {
            return null;
        }
        return resolveClassName(constants, superClass);
    }

    private static int readConstantPool(byte[] data, int offset, Object[] constants) throws IOException {
        int i;

        for (i = 1; i < constants.length; i++) {
            int tag = readU1(data, offset);
            offset++;
            switch (tag) {
                case 1:
                    int length = readU2(data, offset);
                    offset += 2;
                    constants[i] = new String(data, offset, length, "UTF-8");
                    offset += length;
                    break;
                case 7:
                case 8:
                    constants[i] = new Integer(readU2(data, offset));
                    offset += 2;
                    break;
                case 3:
                case 4:
                case 9:
                case 10:
                case 11:
                case 12:
                    offset += 4;
                    break;
                case 5:
                case 6:
                    offset += 8;
                    i++;
                    break;
                default:
                    throw new IOException("Unsupported constant tag " + tag);
            }
        }
        return offset;
    }

    private static String resolveClassName(Object[] constants, int classIndex) {
        Object nameIndex = constants[classIndex];
        if (!(nameIndex instanceof Integer)) {
            return null;
        }
        return (String) constants[((Integer) nameIndex).intValue()];
    }

    private static String className(File root, File file) throws IOException {
        String rootPath = root.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        String relative = filePath.substring(rootPath.length() + 1);
        return relative.substring(0, relative.length() - ".class".length()).replace(File.separatorChar, '.');
    }

    private static byte[] readAll(File file) throws IOException {
        FileInputStream input = new FileInputStream(file);
        byte[] data;
        int offset = 0;

        try {
            data = new byte[(int) file.length()];
            while (offset < data.length) {
                int read = input.read(data, offset, data.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        } finally {
            input.close();
        }
        return data;
    }

    private static int readU1(byte[] data, int offset) {
        return data[offset] & 0xFF;
    }

    private static int readU2(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int readU4(byte[] data, int offset) {
        return (readU2(data, offset) << 16) | readU2(data, offset + 2);
    }
}
