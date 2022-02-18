package org.yangrd.lab.lisp.type;

import lombok.EqualsAndHashCode;
import lombok.Value;

public interface Strings{

    String getVal();

    static Strings of(String str) {
        return new SimpleStr(str);
    }

    @Value
    @EqualsAndHashCode
    class SimpleStr implements Strings {
        String val;
        @Override
        public String toString() {
            return "'" + val+"'";
        }


    }
}
