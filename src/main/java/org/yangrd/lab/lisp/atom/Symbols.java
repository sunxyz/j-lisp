package org.yangrd.lab.lisp.atom;

import lombok.Value;

public interface Symbols extends Atom<String>{
    static Symbols of(String name) {
        return new SimpleSymbols(name.toLowerCase());
    }

    @Value
    class SimpleSymbols implements Symbols {
        String val;

        @Override
        public String toString() {
            return "" + val;
        }
    }
}
