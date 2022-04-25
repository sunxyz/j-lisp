package org.yangrd.lab.lisp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.support.ConsMaker;
import org.yangrd.lab.lisp.support.FileUtils;
import org.yangrd.lab.lisp.type.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
            Object v = env.env(exp.carSymbols()).orElseThrow(() -> new IllegalArgumentException(exp.parent().parent() + ": " + exp.carSymbols() + " not define"));
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

    private static Object eval(Object o, Cons parentExp, Env env) {
        return IS_EXP.test(o) ? eval((Cons) o, env) : (IS_SYMBOLS.test(o) ? eval(ConsMaker.makeSubExp(parentExp, o), env) : o);
    }

    private static Object apply(Object v, Cons cdr, Env env, Object... args) {
        return ((Function<ApplyArgs, Object>) v).apply(ApplyArgs.of(cdr, env, () -> cdr.data().stream().map(o -> eval(o, cdr, env)).toArray(), args.length > 0 ? args : null));
    }

    @AllArgsConstructor(staticName = "of")

    public static class ApplyArgs extends EvalAndApplyProxy {
        @Getter
        final Cons exp;
        @Getter
        final Env env;
        @Getter
        final Supplier<Object[]> lazyArgs;
        @Setter
        Object[] args;

        public Object[] args() {
            if (Objects.isNull(args)) {
                args = lazyArgs.get();
            }
            return args;
        }

        public Cons argsCons() {
            return ConsMaker.makeList(args());
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

    public static class FunManager {
        private static final Map<String, Function<ApplyArgs, Object>> FUNCTIONAL = new ConcurrentHashMap<>();

        static {
            reg("load", FunManager::load);
            reg("lambda", applyArgs -> lambda(applyArgs.getExp(), applyArgs.getEnv(), false));
            reg("lambda-lep", applyArgs -> lambda(applyArgs.getExp(), applyArgs.getEnv(), true));
            reg("quote", applyArgs -> quote(applyArgs.getExp(), applyArgs));

            reg("newline", applyArgs -> {
                System.out.println();
                return Nil.NIL;
            });
            reg("begin", applyArgs -> applyArgs.eval(ConsMaker.makeExp(applyArgs.getExp().parent(), applyArgs.getExp().data().toArray())));
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
            regFileFun();
            reg("nil", applyArgs -> Nil.NIL);
            reg("if", applyArgs -> if0(applyArgs, applyArgs.getExp()));
            reg("cond", applyArgs -> cond(applyArgs.getExp(), applyArgs.getEnv(), applyArgs::eval));
        }


        public static void reg(String optKey, Function<ApplyArgs, Object> opt) {
            FUNCTIONAL.put(optKey, opt);
        }

        private static Function<ApplyArgs, Object> lambda(Cons cdr, Env env, boolean isLazyEvaluation) {
            return new Function<ApplyArgs, Object>() {
                @Override
                public Object apply(ApplyArgs applyArgs) {
                    Cons body = cdr.cdr();
                    Env env0 = Env.newInstance(isLazyEvaluation ? applyArgs.getEnv() : env);
                    bindEnv(applyArgs, env0);
                    return applyArgs.eval(body, env0);
                }

                private void bindEnv(ApplyArgs applyArgs, Env env) {
                    Cons val = isLazyEvaluation ? applyArgs.getExp() : applyArgs.argsCons();
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
//                        System.out.println(args);
                        if (args.carSymbols().equals(Symbols.of("."))) {
                            loopBind(env, args.cdr(), val.data().size() == 1 && IS_EXP.test(val.car()) ? val : ConsMaker.makeList(val));
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
            Object[] args = applyArgs.args();
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
            Object o = IS_EXP.test(car) ? applyArgs.eval(cdr.carCons()) : car;
            boolean isTure = toBoolean(o);
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
                Cons cons = ConsMaker.makeList(Symbols.of("apply"), cdr.cdr().car(), ConsMaker.makeQuote(applyArgs1.getExp().list().toArray()));
                Object apply = applyArgs1.eval(cons);
//                log.debug("marco fun: {} , from: {}", cdr.carSymbols(), apply);
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
                List<Object> list = applyArgs.args != null ? Arrays.asList(applyArgs.args()) : applyArgs.getExp().list();
                List<Object> holder = new ArrayList<>();
                holder.add(null);
                return warp(list.stream().map(o -> {
                    Object t = applyArgs.eval(o, applyArgs.getExp());
                    holder.set(0, t);
                    return t;
                }).allMatch(FunManager::toBoolean) ? holder.iterator().next() : false);
            });
            reg("or", applyArgs -> {
                List<Object> list = applyArgs.args != null ? Arrays.asList(applyArgs.args()) : applyArgs.getExp().list();
                List<Object> holder = new ArrayList<>();
                holder.add(null);
                return warp(list.stream().map(o -> {
                    Object t = applyArgs.eval(o, applyArgs.getExp());
                    holder.set(0, t);
                    return t;
                }).anyMatch(FunManager::toBoolean) ? holder.iterator().next() : false);
            });
            reg("not", applyArgs -> {
                Object[] ts = applyArgs.args();
                validateTrue(ts.length > 0, applyArgs.getExp() + "not args only one");
                return warp(!toBoolean(ts[0]));
            });
            reg("eqv?", applyArgs -> predicate(applyArgs, Object::equals));
            reg("<", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x < (Integer) y : x.toString().length() < y.toString().length()));
            reg("<=", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x <= (Integer) y : x.toString().length() <= y.toString().length()));
            reg("=", applyArgs -> predicate(applyArgs, Object::equals));
            reg("!=", applyArgs -> predicate(applyArgs, (x, y) -> !x.equals(y)));
            reg(">", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x > (Integer) y : x.toString().length() > y.toString().length()));
            reg(">=", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x >= (Integer) y : x.toString().length() >= y.toString().length()));
            reg("boolean->number", applyArgs -> toBoolean(applyArgs.args()[0]) ? 1 : 0);
        }

        private static void regConsFun() {
            reg("cons", applyArgs -> {
                Object[] objects = applyArgs.args();
                return warp(ConsMaker.makeCons(objects[0], objects[1]));
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
            reg("list", applyArgs -> ConsMaker.makeList(applyArgs.args()));
            reg("make-list", applyArgs -> ConsMaker.makeList(new Object[(int) applyArgs.args()[0]]));
            reg("make-boolean-list", applyArgs -> ConsMaker.makeList(new Booleans[(int) applyArgs.args()[0]]));
            reg("list-ref", applyArgs -> {
                Object[] x = applyArgs.args();
                return ((Cons) x[0]).list().get((Integer) x[1]);
            });
            reg("list-tail", applyArgs -> {
                Object[] x = applyArgs.args();
                Cons cons = (Cons) x[0];
                return ConsMaker.makeList(cons.list().subList((Integer) x[1], cons.list().size()).toArray());
            });
            reg("list-set!", applyArgs -> {
                ((Cons) applyArgs.args()[0]).list().set((int) applyArgs.args()[1], applyArgs.args()[2]);
                return Nil.NIL;
            });
            reg("list-add", applyArgs -> {
                ((Cons) applyArgs.args()[0]).add(applyArgs.args()[1]);
                return Nil.NIL;
            });
            reg("list-add-all", applyArgs -> {
                ((Cons) (applyArgs.args()[1])).forEach(o -> ((Cons) applyArgs.args()[0]).add(o));
                return Nil.NIL;
            });
            reg("list-map", applyArgs -> {
                Cons arg = (Cons) applyArgs.args()[0];
                List<Object> list = arg.list();
                Function<ApplyArgs, Object> f = (Function<ApplyArgs, Object>) applyArgs.args()[1];
                List<Object> r = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    r.add(applyArgs.apply(f, arg, applyArgs.getEnv(), Arrays.asList(list.get(i), i, arg).toArray()));
                }
                return ConsMaker.makeList(r.toArray());
            });
            reg("list-foreach", applyArgs -> {
                Cons arg = (Cons) applyArgs.args()[0];
                List<Object> list = arg.list();
                Function<ApplyArgs, Object> f = (Function<ApplyArgs, Object>) applyArgs.args()[1];
                for (int i = 0; i < list.size(); i++) {
                    applyArgs.apply(f, arg, applyArgs.getEnv(), Arrays.asList(list.get(i), i, arg).toArray());
                }
                return Nil.NIL;
            });
            reg("list-sub", applyArgs -> {
                Cons arg = (Cons) applyArgs.args()[0];
                List<Object> list = arg.list();
                return ConsMaker.makeList(list.subList((int) applyArgs.args()[1], (int) applyArgs.args()[2]).toArray());
            });
            reg("null?", applyArgs -> allMath(applyArgs, o -> {
                if (o instanceof Cons) {
                    return ((Cons) o).isEmpty();
                } else {
                    return o instanceof Nil || Objects.isNull(o);
                }
            }));
            reg("pair?", applyArgs -> allMath(applyArgs, o -> o instanceof Cons && ((Cons) o).isCons()));
            reg("list?", applyArgs -> allMath(applyArgs, o -> o instanceof Cons && ((Cons) o).isList()));
            reg("exp?", applyArgs -> allMath(applyArgs, o -> o instanceof Cons));
            reg("cons->arraylist", applyArgs -> warp(applyArgs.args()[0]));
            reg("list->vector", applyArgs -> Vectors.of(((Cons) applyArgs.args()[0]).data().toArray()));
            reg("list->string", applyArgs -> Strings.of(((Cons) applyArgs.args()[0]).data().stream().map(Object::toString).collect(Collectors.joining())));
            reg("list->dict", applyArgs -> {
                Object[] args = applyArgs.args();
                int length = args.length;
                validateTrue(length%2==0,"key val size not eq");
                Cons keys = ConsMaker.makeList();
                Cons values = ConsMaker.makeList();
                for (int i = 0; i < length; i++) {
                    if(i%2==0){
                        keys.add(args[i]);
                    }else{
                        values.add(args[i]);
                    }
                }
                return Dict.of(keys, values);
            });
            reg("length", applyArgs -> {
                Object o = applyArgs.args()[0];
                if (IS_EXP.test(o)) {
                    return ((Cons) o).data().size();
                } else if (o instanceof Strings) {
                    return ((Strings) o).getVal().length();
                } else if (o instanceof Vectors) {
                    return ((Vectors) o).size();
                } else if (o instanceof Dict) {
                    return ((Dict) o).size();
                } else {
                    return 0;
                }
            });

        }

        private static void regSymbolsFun() {
            reg("symbol?", applyArgs -> allMath(applyArgs, o -> o instanceof Symbols));
            reg("gensym", applyArgs -> Symbols.of("gen-" + new Random().nextInt(1024)));
            reg("symbol->string", applyArgs -> Strings.of(((Symbols) applyArgs.args()[0]).getVal()));
        }

        private static void regNumbersFun() {
            reg("number?", applyArgs -> allMath(applyArgs, o -> o instanceof Number));
            reg("integer?", applyArgs -> allMath(applyArgs, o -> o instanceof Integer));
            reg("number->string", applyArgs -> Strings.of(applyArgs.args()[0].toString()));
            reg("sqrt", applyArgs -> Math.round(Math.sqrt(((Integer) (applyArgs.args()[0])).floatValue())));
            reg("+", applyArgs -> toIntStream(applyArgs.args()).reduce(Integer::sum).orElse(null));
            reg("-", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a - b).orElse(null));
            reg("*", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a * b).orElse(null));
            reg("/", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a / b).orElse(null));
            reg("%", applyArgs -> toIntStream(applyArgs.args()).reduce((a, b) -> a % b).orElse(null));
        }

        private static void regVectorsFun() {
            reg("vector?", applyArgs -> allMath(applyArgs, o -> o instanceof Vectors));
            reg("vector", applyArgs -> Vectors.of(applyArgs.args()));
            reg("make-vector", applyArgs -> Vectors.mark((Integer) applyArgs.eval(applyArgs.getExp().car(), applyArgs.getExp())));
            reg("vector-ref", applyArgs -> ((Vectors) applyArgs.args()[0]).ref((Integer) applyArgs.args()[1]));
            reg("vector-set!", applyArgs -> {
                ((Vectors) applyArgs.args()[0]).set((Integer) applyArgs.args()[1], applyArgs.args()[2]);
                return Nil.NIL;
            });
            reg("vector->list", applyArgs -> ConsMaker.makeList(((Vectors) applyArgs.args()[0]).data()));
        }

        private static void regStringsFun() {
            reg("string?", applyArgs -> allMath(applyArgs, o -> o instanceof Strings));
            reg("string->list", applyArgs -> ConsMaker.makeList(Arrays.stream(((Strings) applyArgs.args()[0]).toCharArray()).map(Object::toString).map(Strings::of).toArray()));
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
            reg("string-replace-all-space", applyArgs -> ((Strings) applyArgs.args()[0]).replaceAllSpace());
            reg("string-index-of", applyArgs -> ((Strings) applyArgs.args()[0]).indexOf((Strings)applyArgs.args()[1]));
            reg("string-last-index-of", applyArgs -> ((Strings) applyArgs.args()[0]).lastIndexOf((Strings)applyArgs.args()[1]));
            reg("string-upcase", applyArgs -> ((Strings) applyArgs.args()[0]).upcase());
            reg("string-downcase", applyArgs -> ((Strings) applyArgs.args()[0]).downcase());
            reg("string-trim", applyArgs -> ((Strings) applyArgs.args()[0]).trim());
            reg("string-substitute", applyArgs -> ((Strings) applyArgs.args()[0]).substitute((Strings) applyArgs.args()[1],(Strings) applyArgs.args()[2]));
            reg("string-subseq", applyArgs -> ((Strings) applyArgs.args()[0]).subseq((Integer) applyArgs.args()[1],(Integer) applyArgs.args()[2]));
            reg("string-remove", applyArgs -> ((Strings) applyArgs.args()[0]).remove((Strings) applyArgs.args()[1]));
            reg("string-split", applyArgs -> ((Strings) applyArgs.args()[0]).split((Strings) applyArgs.args()[1]));
        }

        private static void regDict() {
            reg("dict?", applyArgs -> allMath(applyArgs, o -> o instanceof Dict));
            reg("dict", applyArgs -> {
                Object[] args = applyArgs.args();
                if(args.length ==2){
                    return Dict.of((Cons) args[0], (Cons) args[1]);
                }else{
                    return Dict.mark();
                }
            });
            reg("make-dict", applyArgs -> Dict.mark());
            reg("dict-remove!", applyArgs -> ((Dict) applyArgs.args()[0]).remove(applyArgs.args()[1]));
            reg("dict-get", applyArgs -> {
                Object v = ((Dict) applyArgs.args()[0]).get(applyArgs.args()[1]);
                return Objects.isNull(v)?Nil.NIL:v;
            });
            reg("dict-put!", applyArgs -> {
                ((Dict) applyArgs.args()[0]).put(applyArgs.args()[1], applyArgs.args()[2]);
                return Nil.NIL;
            });
            reg("dict-contains?", applyArgs -> warp(((Dict) applyArgs.args()[0]).containsKey(applyArgs.args()[1])));
            reg("dict-keys->list", applyArgs -> ConsMaker.makeList(((Dict) applyArgs.args()[0]).keySet().toArray()));
            reg("dict-values->list", applyArgs -> ConsMaker.makeList(((Dict) applyArgs.args()[0]).values().toArray()));
            reg("dict-items->list", applyArgs -> ConsMaker.makeList(((Dict) applyArgs.args()[0]).entrySet().stream().map(e -> ConsMaker.makeCons(e.getKey(), e.getValue())).toArray()));
        }

        private static void regBaseFun() {
            reg("while", applyArgs -> {
                Cons exp = applyArgs.getExp();
                Object o = Nil.NIL;
                while (toBoolean(applyArgs.eval(exp.car(), exp))) {
                    o = applyArgs.eval(exp.cdr());
                }
                return o;
            });
            reg("error", applyArgs -> {
                throw new IllegalArgumentException(applyArgs.args()[0].toString());
            });
            reg("method?", applyArgs -> allMath(applyArgs, o -> o instanceof Function));
            reg("read-line", applyArgs -> {
                if(applyArgs.args().length>0){
                    Object[] args = applyArgs.args();
                    validateTrue(args[0] instanceof BufferedReader, applyArgs.getExp()+",  arg0 not io");
                    try {
                        String str = ((BufferedReader) args[0]).readLine();
                        return Objects.isNull(str)?Nil.NIL:Strings.of(str);
                    }catch (IOException e){
                        throw new RuntimeException(e);
                    }
                }else {
                    Scanner scanner = new Scanner(System.in);
                    return Strings.of(scanner.next());
                }
            });
            reg("eval", applyArgs -> {
                return eval(((Strings)applyArgs.args()[0]).getVal());
            });
            reg("display", applyArgs -> {
                Object[] args = applyArgs.args();
                Object val = args[0];
                if(args.length>1){
                    if(args[1] instanceof OutputStream){
                        try {
                            ((OutputStream)args[1]).write(val.toString().getBytes(StandardCharsets.UTF_8));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else {
                        System.out.print(val instanceof Function ? "<procedure>" : val);
                    }
                }else{
                    System.out.print(val instanceof Function ? "<procedure>" : val);
                }
                return Nil.NIL;
            });
        }

        private static void regFileFun() {
            // custom
//            reg("read-file-line", applyArgs -> {
//                Object[] args = applyArgs.args();
//                FileUtils.readFileLine(((Strings) args[1]).getVal(), line -> Booleans.of(!applyArgs.apply(args[0], applyArgs.getExp(), applyArgs.getEnv(), line).equals(Booleans.FALSE)), args.length>2?Booleans.TRUE.equals(args[2]):true);
//                return Nil.NIL;
//            });
            reg("call-with-input-file", applyArgs -> {
                Object[] args = applyArgs.args();
                validateTrue(args.length==2,"not args size < 2");
                validateTrue(args[0] instanceof Strings, "arg0 need type string");
                validateTrue(args[1] instanceof Function, "arg1 need type <procedure>");

                FileUtils.readFileLine(((Strings) args[0]).getVal(), reader -> {
                    applyArgs.apply(args[1], applyArgs.getExp(), applyArgs.getEnv(), reader);
                    return Nil.NIL;
                });
                return Nil.NIL;
            });
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
            } else if (o instanceof Character) {
                return !o.equals('0');
            } else if (o instanceof Strings) {
                return  !((Strings) o).getVal().equals("0");
            }else{
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
                Cons list = ConsMaker.makeList();
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