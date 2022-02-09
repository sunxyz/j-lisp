package org.yangrd.lab.lisp.atom;

import lombok.Value;

public interface Symbols {
    static Symbols of(String name) {
        return new SimpleSymbols(name);
    }

    String getName();

    @Value
    class SimpleSymbols implements Symbols {
        String name;

        @Override
        public String toString() {
            return "`" + name;
        }
    }
}
