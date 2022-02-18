package org.yangrd.lab.lisp.type;

import lombok.Value;

public interface Booleans extends Atom<Boolean> {

    Booleans FALSE = SimpleBooleans.of(false);
    Booleans TRUE = SimpleBooleans.of(true);

    static Booleans of(boolean v) {
        return v ? TRUE : FALSE;
    }

    @Value(staticConstructor = "of")
    class SimpleBooleans implements Booleans {
        Boolean val;

        @Override
        public String toString() {
            return val ? "#t" : "#f";
        }
    }

}
