package org.yangrd.lab.lisp.atom;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public interface Vectors {

    Object ref(int index);

    void set(int index, Object v);

    int size();

    static Vectors of(Object[] v){
        return SimpleVectors.of(v);
    }

    static Vectors mark(int size){
        return SimpleVectors.of(new Object[size]);
    }

    @AllArgsConstructor(staticName = "of")
    class SimpleVectors implements Vectors{

        private final Object[] objects;

        @Override
        public Object ref(int index) {
            return objects[index];
        }

        @Override
        public void set(int index, Object v) {
            objects[index] =v;
        }

        @Override
        public int size() {
            return objects.length;
        }

        @Override
        public String toString() {
            Optional<String> reduce = Stream.of(objects).map(o -> Objects.isNull(o) ? "nil" : o).map(Object::toString).reduce((x, y) -> x + " " + y).map(o -> "#(" + o + ")");
            return reduce.orElse("#()");
        }
    }
}
