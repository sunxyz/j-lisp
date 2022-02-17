package org.yangrd.lab.lisp.support;

import org.yangrd.lab.lisp.Cons;
import org.yangrd.lab.lisp.atom.Symbols;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public final class ConsMarker {

    public static Cons markExp(Cons parent, Object... data) {
        return Cons.of(new ArrayList<>(Arrays.asList(data)), parent, Cons.ConsType.EXP);
    }

    public static Cons markSubExp(Cons parent, Object obj) {
        return Cons.of(Collections.singletonList(obj), parent, Cons.ConsType.SUB_EXP);
    }

    public static Cons markList(Object... data) {
        return Cons.of(new ArrayList<>(Arrays.asList(data)), null, Cons.ConsType.LIST);
    }

    public static Cons markCons(Object o, Object o2) {
        return Cons.of(Arrays.asList(o, o2), null, Cons.ConsType.CONS);
    }

    public static Cons markQuote(Object... data) {
        Cons quote = Cons.of(new ArrayList<>(Collections.singletonList(Symbols.of("quote"))), null, Cons.ConsType.QUOTE);
        if (data.length > 0) {
            quote.add(markExp(quote, data));
        }
        return quote;
    }
}
