package org.yangrd.lab.lisp.atom;

import lombok.Value;

public interface Strings{

    String getVal();

    static Strings of(String str) {
        return new SimpleStr(str);
    }

    @Value
    class SimpleStr implements Strings {
        String val;
        @Override
        public String toString() {
            return "'" + val+"'";
        }
    }
}
