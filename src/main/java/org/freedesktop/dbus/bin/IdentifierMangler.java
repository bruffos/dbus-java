//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.freedesktop.dbus.bin;

import java.util.Arrays;

public final class IdentifierMangler {
    private static String[] keywords = new String[]{"class", "true", "false", "null", "abstract", "continue", "for", "new", "switch", "assert", "default", "goto", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw", "byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "strictfp", "volatile", "const", "float", "native", "super", "while"};

    public static String mangle(String name) {
        if(Arrays.binarySearch(keywords, name.toLowerCase()) >= 0) {
            name = "_" + name;
        }

        return name;
    }

    private IdentifierMangler() {
    }

    static {
        Arrays.sort(keywords);
    }
}
