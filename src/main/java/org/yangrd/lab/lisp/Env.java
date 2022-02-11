package org.yangrd.lab.lisp;


import org.yangrd.lab.lisp.atom.Symbols;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Env {

    private final Map<String, Object> env = new HashMap<>();
    private Env parent;

    public static Env newInstance(Env parent) {
        Env env1 = new Env();
        env1.parent = parent;
        return env1;
    }

    public void setEnv(Symbols symbols, Object val) {
       setEnv(symbols.getVal(),val);
    }

    public void setEnv(String key, Object val) {
        env.put(key, val);
    }

    public Optional<Object> env(Symbols symbols) {
        String symbolsName = symbols.getVal();
        return Optional.ofNullable(env.containsKey(symbolsName) ? env.get(symbolsName) : (parent != null ? parent.env(symbols).orElse(null) : null));
    }

    public boolean noContains(Symbols symbols){
        return !env.containsKey(symbols.getVal());
    }

    public Env parent(){
        return parent;
    }
}
