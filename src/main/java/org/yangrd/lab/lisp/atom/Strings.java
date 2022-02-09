package org.yangrd.lab.lisp.atom;

import lombok.Value;

public interface Strings {

    static Strings of(String str) {
        return new SimpleStr(str);
    }

    String getString();

    @Value
    class SimpleStr implements Strings {
        String string;
        @Override
        public String toString() {
            return "'" + string+"'";
        }
    }
}
