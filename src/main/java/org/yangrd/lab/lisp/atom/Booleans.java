package org.yangrd.lab.lisp.atom;

public interface Booleans extends Atom<Boolean>{

    static Booleans of(Boolean v){
            return new Booleans() {
                @Override
                public Boolean getVal() {
                    return v;
                }

                @Override
                public String toString() {
                    return v?"#t":"#f";
                }
            };
    }

}
