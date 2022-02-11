package org.yangrd.lab.lisp;

import lombok.RequiredArgsConstructor;
import org.yangrd.lab.lisp.atom.Symbols;

import java.util.*;


@RequiredArgsConstructor(staticName = "of")
public class Cons implements Iterable<Object> {

    private final List<Object> data;

    private final Cons parent;

    private final boolean exp;

    public static Cons newInstance(Cons parent) {
        return Cons.of(new ArrayList<>(), parent, true);
    }

    public Cons add(Object obj) {
        data.add(obj);
        return this;
    }

    public Object car() {
        return iterator().next();
    }

    public Cons carCons() {
        return (Cons) car();
    }

    public Symbols carSymbols() {
        return (Symbols) car();
    }

    public Cons cdr() {
        return Cons.of(data.subList(1, data.size() ), this,false);
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

    public boolean isExp() {
        return exp;
    }

    public boolean isEmpty(){
        return data.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return data.iterator();
    }

    @Override
    public String toString() {
        Optional<String> reduce = data.stream().map(Object::toString).reduce((x, y) -> x + " " + y).map(o -> "(" + o + ")");
        return reduce.orElse("()");
    }
}