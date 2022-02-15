package org.yangrd.lab.lisp;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.yangrd.lab.lisp.atom.Symbols;

import java.util.*;

import static org.yangrd.lab.lisp.Cons.ConsType.*;


@AllArgsConstructor
public class Cons implements Iterable<Object> {

    private final List<Object> data;

    private Cons parent;

    private final ConsType type;

    protected static final Cons EMPTY = newInstance(null);

    public static Cons newInstance(Cons parent) {
        return Cons.of(new ArrayList<>(), parent, EXP);
    }

    public static Cons of(List<Object> data, Cons parent, ConsType type) {
        Cons cons = new Cons(data, parent, type);
        if(Objects.nonNull(data)){
            data.stream().filter(o -> o instanceof Cons&& ((Cons) o).type.equals(CONS)).forEach(o -> ((Cons) o).parent=cons);
        }
        return cons;
    }

    public Cons add(Object obj) {
        data.add(obj);
        return this;
    }

    public Object car() {
        return data.isEmpty()?EMPTY:iterator().next();
    }

    public Cons carCons() {
        return (Cons) car();
    }

    public Symbols carSymbols() {
        return (Symbols) car();
    }

    public Cons cdr() {
        return isCons()?(Cons)data.get(1): Cons.of(data.subList(1, data.size() ), this,SUB_EXP);
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


    public boolean isEmpty(){
        return data.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return data.iterator();
    }

    public Cons cloneEmpty(){
        return Cons.of(new ArrayList<>(), parent, type);
    }

    @Override
    public String toString() {
//        if(type.equals(CONS)){
//            return  data.stream().filter(o ->  (!(o instanceof Cons) )|| !((Cons) o).isEmpty()).map(Object::toString).reduce((x, y) -> x + " " + y).map(o->this.parent!=null?o:"(" + o + ")").orElse("()");
//        }
        Optional<String> reduce = data.stream().map(Object::toString).reduce((x, y) -> x + " " + y).map(o -> "(" + o + ")");
        return reduce.orElse("()");
    }

    enum ConsType{
        EXP,
        SUB_EXP,
        LIST,
        CONS,
        QUOTE
    }
}