package org.yangrd.lab.lisp;

import lombok.AllArgsConstructor;
import org.yangrd.lab.lisp.atom.Symbols;

import java.util.*;

import static org.yangrd.lab.lisp.Cons.ConsType.*;


@AllArgsConstructor(staticName = "of")
public class Cons implements Iterable<Object> {

    protected static final Cons EMPTY = newInstance(null);
    private final List<Object> data;
    private Cons parent;
    private final ConsType type;

    public static Cons newInstance(Cons parent) {
        return Cons.of(new ArrayList<>(), parent, EXP);
    }

    public void add(Object obj) {
        data.add(obj);
    }

    public Object car() {
        return data.isEmpty() ? EMPTY : iterator().next();
    }

    public Cons carCons() {
        return (Cons) car();
    }

    public Symbols carSymbols() {
        return (Symbols) car();
    }

    public Cons cdr() {
        return isCons() ? (data.get(1) instanceof Cons ? (Cons) data.get(1) : Cons.of(Collections.singletonList(data.get(1)), null, EXP)) : Cons.of(data.subList(1, data.size()), this, SUB_EXP);
    }

    public Collection<Object> data() {
        return Collections.unmodifiableList(data);
    }

    public List<Object> list() {
        return data;
    }

    public Cons parent() {
        return parent;
    }

    public boolean isSubExp() {
        return SUB_EXP.equals(type);
    }

    public boolean isCons() {
        return CONS.equals(type);
    }

    public boolean isList() {
        return LIST.equals(type);
    }

    public boolean isQuote() {
        return QUOTE.equals(type);
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return data.iterator();
    }

    @Override
    public String toString() {
        Optional<String> reduce = data.stream().map(o -> Objects.isNull(o) ? "nil" : o).map(Object::toString).reduce((x, y) -> x + " " + y).map(o -> "(" + o + ")");
        return reduce.orElse("()");
    }

    public enum ConsType {
        EXP,
        SUB_EXP,
        LIST,
        CONS,
        QUOTE
    }
}