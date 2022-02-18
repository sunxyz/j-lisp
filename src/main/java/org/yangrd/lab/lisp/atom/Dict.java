package org.yangrd.lab.lisp.atom;

import org.yangrd.lab.lisp.Cons;

import java.util.HashMap;
import java.util.Map;

public interface Dict extends Map<Object,Object>  {

    static Dict mark(){
        return new SimpleDict();
    }

    static Dict of(Cons keys, Cons values){
        SimpleDict simpleDict = new SimpleDict();
//        keys.list().size()==values.list().size();
        int size = keys.list().size();
        for(int i=0;i<size;i++){
            simpleDict.put(keys.list().get(i),values.list().get(i));
        }
        return simpleDict;
    }

    class SimpleDict extends HashMap<Object,Object> implements Dict {

    }
}
