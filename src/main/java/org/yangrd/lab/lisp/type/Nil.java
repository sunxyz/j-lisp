package org.yangrd.lab.lisp.type;

public interface Nil extends Atom<String> {
     Nil NIL = new Nil() {
        @Override
        public String getVal() {
            return "";
        }

        @Override
        public String toString() {
            return "nil";
        }
    };
}
