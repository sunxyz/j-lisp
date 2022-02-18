package org.yangrd.lab.lisp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.type.*;
import org.yangrd.lab.lisp.support.ConsMarker;
import org.yangrd.lab.lisp.support.FileUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JLispInterpreter3 {

    private static final Predicate<Object> IS_EXP = o -> o instanceof Cons;

    private static final Predicate<Object> IS_SYMBOLS = o -> o instanceof Symbols;

    private static final Predicate<Object> IS_FUN = o -> o instanceof Function;

    private static final Predicate<Object> IS_ATOM = o -> o instanceof Atom;

    private static final BiPredicate<Cons, Object> CAN_APPLY = (exp, v) -> IS_FUN.test(v) && !exp.isSubExp();

    public static Object eval(String exp) {
        Env root = Env.newInstance(null);
        FunManager.FUNCTIONAL.forEach(root::setEnv);
        return eval(exp, Env.newInstance(root));
    }

    private static Object eval(String exp, Env env) {
        return eval(Parse.parse(exp), env);
    }

    private static Object eval(Cons exp, Env env) {
        Object car = exp.car();
        Cons cdr = exp.cdr();
        if (IS_EXP.test(car)) {
            Object v = eval(exp.carCons(), env);
            if (CAN_APPLY.test(exp, v)) {
                return apply(v, cdr, env);
            }
            return cdr.isEmpty() ? v : eval(cdr, env);
        } else if (IS_SYMBOLS.test(car)) {
            Object v = env.env(exp.carSymbols()).orElseThrow(() -> new IllegalArgumentException(exp.parent() + ": " + exp.carSymbols() + " not define"));
            if (CAN_APPLY.test(exp, v)) {
                return apply(v, cdr, env);
            }
            return cdr.isEmpty() ? v : eval(cdr, env);
        } else if (IS_ATOM.test(car)) {
            return cdr.isEmpty() ? car : eval(cdr, env);
        } else {
            return cdr.isEmpty() ? car : eval(cdr, env);
        }
    }

    private static Object eval(Object o, Cons exp, Env env) {
        return IS_EXP.test(o) ? eval((Cons) o, env) : (IS_SYMBOLS.test(o) ? eval(ConsMarker.markSubExp(exp, o), env) : o);
    }

    private static Object apply(Object v, Cons cdr, Env env, Object... args) {
        return ((Function<ApplyArgs, Object>) v).apply(ApplyArgs.of(cdr, env, () -> args.length > 0 ? args : cdr.data().stream().map(o -> eval(o, cdr, env)).toArray()));
    }

    @RequiredArgsConstructor(staticName = "of")
    @Getter
    public static class ApplyArgs extends EvalAndApplyProxy {
        final Cons exp;
        final Env env;
        final Supplier<Object[]> lazyArgs;
        private Object[] args;

        public Object[] args() {
            if (Objects.isNull(args)) {
                args = lazyArgs.get();
            }
            return args;
        }

        public Cons argsCons() {
            return ConsMarker.markList(args());
        }
    }

    public static abstract class EvalAndApplyProxy {

        public abstract Env getEnv();

        public Object eval(Cons exp, Env... envs) {
            return JLispInterpreter3.eval(exp, envs.length > 0 ? envs[0] : getEnv());
        }

        public Object eval(Object o, Cons exp, Env... envs) {
            return JLispInterpreter3.eval(o, exp, envs.length > 0 ? envs[0] : getEnv());
        }

        public Object eval(String exp) {
            return JLispInterpreter3.eval(exp, getEnv());
        }

        public Object apply(Object v, Cons cdr, Env env, Object... args) {
            return JLispInterpreter3.apply(v, cdr, env, args);
        }
    }

    static class FunManager {
        private static final Map<String, Function<ApplyArgs, Object>> FUNCTIONAL = new ConcurrentHashMap<>();

        static {
            reg("+", applyArgs -> toIntStream(applyArgs.args()).reduce(Integer::sum).orElse(null));
            reg("-", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a - b).orElse(null));
            reg("*", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a * b).orElse(null));
            reg("/", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a / b).orElse(null));
            reg("load", FunManager::load);
            reg("lambda", applyArgs -> lambda(applyArgs.getExp(), applyArgs.getEnv()));
            reg("quote", applyArgs -> quote(applyArgs.getExp(), applyArgs));
            reg("display", applyArgs -> {
                Object val = applyArgs.eval(applyArgs.getExp());
                System.out.print(val instanceof Function ? "<procedure>" : val);
                return Nil.NIL;
            });
            reg("newline", applyArgs -> {
                System.out.println();
                return Nil.NIL;
            });
            reg("begin", applyArgs -> applyArgs.eval(ConsMarker.markExp(applyArgs.getExp().parent(), applyArgs.getExp().data().toArray())));
            reg("define", FunManager::define);
            reg("define-macro", FunManager::defineMacro);
            reg("let", applyArgs -> let(applyArgs.getExp(), applyArgs.getEnv(), applyArgs::eval));
            reg("set!", applyArgs -> set(applyArgs.getExp(), applyArgs.getEnv(), applyArgs::eval));
            reg("apply", applyArgs -> apply0(applyArgs, applyArgs.getExp(), applyArgs.getEnv()));
            regBooleanFun();
            regNumbersFun();
            regSymbolsFun();
            regVectorsFun();
            regStringsFun();
            regConsFun();
            regDict();
            regBaseFun();
            reg("nil", applyArgs -> Nil.NIL);
            reg("if", applyArgs -> if0(applyArgs, applyArgs.getExp()));
            reg("cond", applyArgs -> cond(applyArgs.getExp(), applyArgs.getEnv(), applyArgs::eval));
        }

        private static void reg(String optKey, Function<ApplyArgs, Object> opt) {
            FUNCTIONAL.put(optKey, opt);
        }

        private static Function<ApplyArgs, Object> lambda(Cons cdr, Env env) {
            return new Function<ApplyArgs, Object>() {
                @Override
                public Object apply(ApplyArgs applyArgs) {
                    Cons body = cdr.cdr();
                    Env env0 = Env.newInstance(env);
                    bindEnv(applyArgs, env0);
                    return applyArgs.eval(body, env0);
                }

                private void bindEnv(ApplyArgs applyArgs, Env env) {
                    Cons val = applyArgs.argsCons();
                    // 看是否有可变参数
                    List<Object> args0 = cdr.carCons().list();
                    int argsSize = args0.size();
                    boolean indefiniteLengthArgsFlag = argsSize > 1 && ((Symbols) args0.get(argsSize - 2)).getVal().equals(".");
                    validateTrue((indefiniteLengthArgsFlag && val.data().size() >= argsSize - 2) || args0.size() <= val.data().size(), cdr.parent() + "参数不一致");
                    //参数值
                    Cons args = cdr.carCons();
                    loopBind(env, args, val);
                }

                private void loopBind(Env env, Cons args, Cons val) {
                    if (!args.isEmpty()) {
                        if (args.carSymbols().equals(Symbols.of("."))) {
                            loopBind(env, args.cdr(), val.data().size() == 1 && IS_EXP.test(val.car()) ? val : ConsMarker.markList(val));
                        } else {
                            env.setEnv(args.carSymbols(), val.car());
                            if (!args.cdr().isEmpty()) {
                                loopBind(env, args.cdr(), val.cdr());
                            }
                        }
                    }
                }

                @Override
                public String toString() {
                    return "<procedure>";
                }
            };
        }

        private static Object quote(Cons cdr, ApplyArgs applyArgs) {
            // 支持 ， ,@
            //,紧跟的表达式需要eval  ,@ 紧跟的表达式需要先 eval 在 flatMap
            Object car = cdr.car();
            if (IS_EXP.test(car)) {
                Cons cons = cdr.carCons();
                return quoteRender(cons, cdr, applyArgs);
            } else {
                return car;
            }
        }

        private static Cons quoteRender(Cons cons, Cons parent, ApplyArgs applyArgs) {
            Symbols s0 = Symbols.of(",");
            Symbols s1 = Symbols.of(",@");
            Cons exps = Cons.of(new ArrayList<>(), null, Cons.ConsType.QUOTE);
            Object pre = null;
            for (Object o : cons) {
                if (s0.equals(pre)) {
                    exps.add(applyArgs.eval(o, parent));
                } else if (s1.equals(pre)) {
                    Object obj = applyArgs.eval(o, parent);
                    validateTrue(IS_EXP.test(obj), s1 + " The return value of the last element needs to be an list");
                    ((Cons) obj).data().forEach(exps::add);
                } else if (!s0.equals(o) && !s1.equals(o)) {
                    if (IS_EXP.test(o)) {
                        exps.add(quoteRender((Cons) o, (Cons) o, applyArgs));
                    } else {
                        exps.add(o);
                    }
                }
                pre = o;
            }
            return exps;
        }

        private static Object load(ApplyArgs applyArgs) {
            Cons args = applyArgs.getExp();
            Object o = Nil.NIL;
            for (Object d : args) {
                validateTrue(d instanceof Strings, d + " type error");
                String file = ((Strings) d).getVal();
                String str = FileUtils.readFile(file);
                o = applyArgs.eval(str);
            }
            return o;
        }

        private static Object if0(ApplyArgs applyArgs, Cons cdr) {
            Object car = cdr.car();
            boolean isTure = toBoolean(IS_EXP.test(car) ? applyArgs.eval(cdr.carCons()) : car);
            if (isTure) {
                Object then = cdr.cdr().car();
                return applyArgs.eval(then, cdr.cdr());
            } else {
                if (cdr.cdr().cdr().data().size() > 0) {
                    return applyArgs.eval(cdr.cdr().cdr());
                }
                return Nil.NIL;
            }
        }

        private static Object cond(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            Cons car = cdr.carCons();
            Cons predicateExp = car.carCons();
            Cons body = car.cdr();
            if (toBoolean(eval.apply(predicateExp, env))) {
                return eval.apply(body, env);
            } else {
                Cons elseCdr = cdr.cdr();
                if (elseCdr.data().size() == 1) {
                    // 去掉括號
                    while (IS_EXP.test(elseCdr.car()) && elseCdr.data().size() == 1) {
                        elseCdr = elseCdr.carCons();
                    }
                    validateTrue(IS_SYMBOLS.test(elseCdr.car()) && elseCdr.carSymbols().getVal().equals("else"), "cond last item not else key");
                    return eval.apply(elseCdr.cdr(), env);
                }
                return cond(elseCdr, env, eval);
            }
        }

        private static Object define(ApplyArgs applyArgs) {
            Cons cdr = applyArgs.getExp();
            Object val = applyArgs.eval(cdr.cdr());
            Env env = applyArgs.getEnv();
            Symbols symbols = cdr.carSymbols();
            validateTrue(env.noContains(symbols), "Do not repeat the definition " + symbols);
            applyArgs.getEnv().setEnv(symbols, val);
            return Nil.NIL;
        }

        private static Object defineMacro(ApplyArgs applyArgs) {
            Cons cdr = applyArgs.getExp();
            validateTrue(applyArgs.getEnv().noContains(cdr.carSymbols()), "Do not repeat the definition " + cdr.carSymbols());
            Function<ApplyArgs, Object> applyFun = (applyArgs1) -> {
                Cons cons = ConsMarker.markList(Symbols.of("apply"), cdr.cdr().car(), ConsMarker.markQuote(applyArgs1.getExp().list().toArray()));
                Object apply = applyArgs1.eval(cons);
                log.debug("marco fun: {} , from: {}", cdr.carSymbols(), apply);
                return applyArgs1.eval(apply, applyArgs1.getExp(), applyArgs1.getEnv());
            };
            applyArgs.getEnv().setEnv(cdr.carSymbols(), applyFun);
            return Nil.NIL;
        }

        private static Object let(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            Object car0 = cdr.car();
            validateTrue(car0 instanceof Cons && cdr.data().size() > 1, "please check" + car0);
            Env env0 = Env.newInstance(env);
            Cons car = cdr.carCons();
            Cons body = cdr.cdr();
            while (!car.isEmpty()) {
                Cons item = car.carCons();
                Symbols var = item.carSymbols();
                validateTrue(env0.noContains(var), "Do not repeat the let " + var);
                env0.setEnv(var, eval.apply(item.cdr(), env));
                car = car.cdr();
            }
            return eval.apply(body, env0);
        }

        private static Object set(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            Symbols var = cdr.carSymbols();
            Object val = eval.apply(cdr.cdr(), env);
            validateTrue(env.env(var).isPresent(), " not definition set! error " + var);
            Env envParent = env;
            while (envParent.noContains(var)) {
                envParent = envParent.parent();
            }
            envParent.setEnv(var, val);
            return Nil.NIL;
        }

        private static Object apply0(ApplyArgs applyArgs, Cons cdr, Env env) {
            Object f = cdr.car();
            f = applyArgs.eval(f, cdr);
            if (IS_FUN.test(f)) {
                return applyArgs.apply(f, cdr, env, flatMapArgs(applyArgs, cdr.cdr()));
            } else {
                return applyArgs.eval(f, cdr);
            }
        }

        private static Object[] flatMapArgs(ApplyArgs applyArgs, Cons args) {
            List<Object> list = args.data().stream().map(o -> applyArgs.eval(o, args)).collect(Collectors.toList());
            List<Object> res = list.subList(0, list.size() - 1);
            Object last = list.get(list.size() - 1);
            if (IS_EXP.test(last)) {
                Cons last1 = (Cons) last;
                if (last1.isList() || last1.isQuote()) {
                    last1.forEach(res::add);
                } else {
                    res.add(last);
                }
            } else {
                res.add(last);
            }
            return res.toArray();
        }

        private static void regBooleanFun() {
            reg("and", applyArgs -> {
                Object[] ts = applyArgs.args();
                return warp(Stream.of(ts).allMatch(FunManager::toBoolean) ? ts[ts.length - 1] : false);
            });
            reg("or", applyArgs -> warp(Stream.of(applyArgs.args()).filter(FunManager::toBoolean).findFirst().orElse(false)));
            reg("not", applyArgs -> {
                Object[] ts = applyArgs.args();
                validateTrue(ts.length == 1, applyArgs.getExp() + "not args only one");
                return warp(!toBoolean(ts[0]));
            });
            reg("eqv?", applyArgs -> predicate(applyArgs, Object::equals));
            reg("<", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x < (Integer) y : x.toString().length() < y.toString().length()));
            reg("<=", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x <= (Integer) y : x.toString().length() <= y.toString().length()));
            reg("=", applyArgs -> predicate(applyArgs, Object::equals));
            reg("!=", applyArgs -> predicate(applyArgs, (x, y) -> !x.equals(y)));
            reg(">", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x > (Integer) y : x.toString().length() > y.toString().length()));
            reg(">=", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x >= (Integer) y : x.toString().length() >= y.toString().length()));
        }

        private static void regConsFun() {
            reg("cons", applyArgs -> {
                Object[] objects = applyArgs.args();
                return warp(ConsMarker.markCons(objects[0], objects[1]));
            });
            reg("car", applyArgs -> ((Cons) (applyArgs.args()[0])).car());
            reg("cdr", applyArgs -> {
                Cons x = (Cons) applyArgs.args()[0];
                return x.isCons() ? x.list().get(1) : x.cdr();
            });
            reg("set-car!", applyArgs -> {
                Object[] x = applyArgs.args();
                ((Cons) x[0]).list().set(0, x[1]);
                return null;
            });
            reg("set-cdr!", applyArgs -> {
                Object[] x = applyArgs.args();
                ((Cons) x[0]).list().set(1, x[1]);
                return null;
            });
            reg("list", applyArgs -> ConsMarker.markList(applyArgs.args()));
            reg("mark-list", applyArgs -> ConsMarker.markList());
            reg("list-ref", applyArgs -> {
                Object[] x = applyArgs.args();
                return ((Cons) x[0]).list().get((Integer) x[1]);
            });
            reg("list-tail", applyArgs -> {
                Object[] x = applyArgs.args();
                Cons cons = (Cons) x[0];
                return ConsMarker.markList(cons.list().subList((Integer) x[1], cons.list().size()));
            });
            reg("list-add", applyArgs ->{ ((Cons)applyArgs.args()[0]).add(applyArgs.args()[1]);return Nil.NIL;});
            reg("list-add-all", applyArgs ->{ ((Cons)(applyArgs.args()[1])).forEach(o->((Cons)applyArgs.args()[0]).add(o));return Nil.NIL;});
            reg("list-map", applyArgs ->{
                Cons arg = (Cons) applyArgs.args()[0];
                List<Object> list = arg.list();
                Function<ApplyArgs,Object> f = ( Function<ApplyArgs,Object>)applyArgs.args()[1];
                List<Object> r = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    r.add(applyArgs.apply(f, arg, applyArgs.getEnv(), Arrays.asList(list.get(i),i,arg).toArray()));
                }
                return ConsMarker.markList(r.toArray());
            });
            reg("null?", applyArgs -> allMath(applyArgs,o-> {
                if(o instanceof Cons){
                    return ((Cons) o).isEmpty();
                }else {
                    return o instanceof Nil || Objects.isNull(o);
                }
            }));
            reg("pair?", applyArgs -> allMath(applyArgs,o -> o instanceof Cons && ((Cons) o).isCons()));
            reg("list?", applyArgs -> allMath(applyArgs,o -> o instanceof Cons && ((Cons) o).isList()));
            reg("exp?", applyArgs -> allMath(applyArgs,o -> o instanceof Cons));
            reg("cons->arraylist", applyArgs -> warp(applyArgs.args()[0]));
            reg("list->vector", applyArgs -> Vectors.of(((Cons)applyArgs.args()[0]).data().toArray()));
            reg("length", applyArgs -> {
                Object o = applyArgs.args()[0];
                if (IS_EXP.test(o)) {
                    return ((Cons) o).data().size();
                } else if (o instanceof Strings) {
                    return ((Strings) o).getVal().length();
                } else if (o instanceof Vectors) {
                    return ((Vectors) o).size();
                } else if(o instanceof Dict){
                   return ((Dict)o).size();
                }else {
                    return 0;
                }
            });

        }

        private static void regSymbolsFun() {
            reg("symbol?", applyArgs -> allMath(applyArgs,o->o instanceof Symbols));
            reg("gensym", applyArgs -> Symbols.of("gen-" + new Random().nextInt(1024)));
            reg("symbol->string", applyArgs -> Strings.of(((Symbols) applyArgs.args()[0]).getVal()));
        }

        private static void regNumbersFun() {
            reg("number?", applyArgs -> allMath(applyArgs,o->o instanceof Number));
            reg("integer?", applyArgs -> allMath(applyArgs,o->o instanceof Integer));
            reg("number->string", applyArgs -> Strings.of(applyArgs.args()[0].toString()));
        }

        private static void regVectorsFun() {
            reg("vector?", applyArgs ->allMath(applyArgs,o->o instanceof Vectors));
            reg("vector", applyArgs -> Vectors.of(applyArgs.argsCons().data().stream().map(o -> applyArgs.eval(o, applyArgs.getExp())).toArray()));
            reg("make-vector", applyArgs -> Vectors.mark((Integer) applyArgs.eval(applyArgs.getExp().car(), applyArgs.getExp())));
            reg("vector-ref", applyArgs -> ((Vectors) applyArgs.args()[0]).ref((Integer) applyArgs.args()[1]));
            reg("vector-set!", applyArgs -> {
                ((Vectors) applyArgs.args()[0]).set((Integer) applyArgs.args()[1], applyArgs.args()[2]);
                return Nil.NIL;
            });
            reg("vector->list",applyArgs -> ConsMarker.markList(((Vectors)applyArgs.getArgs()[0]).data()));
        }

        private static void regStringsFun() {
            reg("string?", applyArgs -> allMath(applyArgs,o-> o instanceof Strings ));
            reg("string->list", applyArgs -> ConsMarker.markList(applyArgs.getArgs()[0]));
            reg("string->number", applyArgs -> {
                try {
                    return Integer.valueOf(((Strings) applyArgs.args()[0]).getVal());
                } catch (RuntimeException e) {
                    log.warn(e.getLocalizedMessage());
                    return Booleans.of(false);
                }
            });
            reg("string->symbol", applyArgs -> Symbols.of(((Strings) applyArgs.args()[0]).getVal()));
            reg("string-append", applyArgs -> warp(Arrays.stream(applyArgs.args()).map(o -> (Strings) o).map(Strings::getVal).collect(Collectors.joining())));
        }

        private static void regDict(){
            reg("dict?",applyArgs ->allMath(applyArgs,o->o instanceof Dict));
            reg("dict", applyArgs ->Dict.of((Cons) applyArgs.args()[0],(Cons) applyArgs.args()[1]));
            reg("make-dict", applyArgs ->Dict.mark());
            reg("dict-remove!", applyArgs ->((Dict)applyArgs.args()[0]).remove(applyArgs.args()[1]));
            reg("dict-get", applyArgs ->((Dict)applyArgs.args()[0]).get(applyArgs.args()[1]));
            reg("dict-put!", applyArgs ->{
                ((Dict)applyArgs.args()[0]).put(applyArgs.args()[1],applyArgs.args()[2]);return Nil.NIL;});
            reg("dict-contains?", applyArgs ->warp(((Dict)applyArgs.args()[0]).containsKey(applyArgs.args()[1])));
            reg("dict-keys->list", applyArgs -> ConsMarker.markList (((Dict)applyArgs.args()[0]).keySet().toArray()));
            reg("dict-values->list", applyArgs -> ConsMarker.markList (((Dict)applyArgs.args()[0]).values().toArray()));
            reg("dict-items->list", applyArgs -> ConsMarker.markList (((Dict)applyArgs.args()[0]).entrySet().stream().map(e->ConsMarker.markCons(e.getKey(),e.getValue())).toArray()));
        }

        private static void regBaseFun(){
            reg("while", applyArgs -> {
                Cons exp = applyArgs.getExp();
                Object o = Nil.NIL;
                while (toBoolean(applyArgs.eval(exp.car(),exp))){
                    o  = applyArgs.eval(exp.cdr());
                }
                return o;
            });
            reg("error", applyArgs -> {
                throw new IllegalArgumentException(applyArgs.args()[0].toString());
            });
            reg("method?", applyArgs -> allMath(applyArgs,o->o instanceof Function));
        }

        private static Object allMath(ApplyArgs applyArgs, Predicate<Object> predicates) {
            Object[] objs = applyArgs.args();
            validateTrue(objs.length > 0, applyArgs.getExp() + " args qty > 0 ");
            return warp(Arrays.stream(objs).allMatch(predicates));
        }

        private static Object predicate(ApplyArgs applyArgs, BiPredicate<Object, Object> predicates) {
            Object[] objs = applyArgs.args();
            validateTrue(objs.length > 1, applyArgs.getExp() + " args qty > 1 ");
            Object o = objs[0];
            for (int i = 1; i < objs.length; i++) {
                Object o1 = objs[i];
                boolean b = predicates.test(o, o1);
                if (!b) {
                    return warp(false);
                }
                o = o1;
            }
            return warp(true);
        }

        private static boolean toBoolean(Object o) {
            if (o instanceof Boolean) {
                return (Boolean) o;
            } else if (o instanceof Booleans) {
                return ((Booleans) o).getVal();
            } else if (o instanceof Cons) {
                return !((Cons) o).isEmpty();
            } else {
                return !o.equals(0);
            }
        }

        private static Object warp(Object o) {
            if (o instanceof String) {
                return Strings.of((String) o);
            }
            if (o instanceof Boolean) {
                return Booleans.of((Boolean) o);
            }
            if (o instanceof Cons && ((Cons) o).isCons()) {
                Cons list = ConsMarker.markList();
                Cons x = (Cons) o;
                if (x.data().size() > 1 && IS_EXP.test(x.list().get(1)) && (x.cdr().isCons() || x.cdr().isList())) {
                    while (!x.isEmpty()) {
                        list.add(x.car());
                        if (x.data().size() == 1) {
                            x = Cons.EMPTY;
                        } else if (IS_EXP.test(x.list().get(1))) {
                            x = x.cdr();
                        } else {
                            list.add(x.list().get(1));
                            x = Cons.EMPTY;
                        }

                    }
                    return list;
                }
            }
            return o;
        }

        private static void validateTrue(boolean flag, String err) {
            if (!flag) {
                throw new IllegalArgumentException(err);
            }
        }

        private static Stream<Integer> toIntStream(Object[] objs) {
            return Stream.of(objs).map(Object::toString).map(Integer::valueOf);
        }
    }
}