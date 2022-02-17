package org.yangrd.lab.lisp;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.atom.Atom;
import org.yangrd.lab.lisp.atom.Booleans;
import org.yangrd.lab.lisp.atom.Strings;
import org.yangrd.lab.lisp.atom.Symbols;
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

    @EqualsAndHashCode(callSuper = false)
    @Value(staticConstructor = "of")
    public static class ApplyArgs extends EvalAndApplyProxy {
        Cons exp;
        Env env;
        Supplier<Object[]> lazyArgs;

        public Object[] args() {
            return lazyArgs.get();
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
                return null;
            });
            reg("newline", applyArgs -> {
                System.out.println();
                return null;
            });
            reg("begin", applyArgs -> applyArgs.eval(ConsMarker.markExp(applyArgs.getExp().parent(), applyArgs.getExp().data().toArray())));
            reg("define", FunManager::define);
            reg("define-macro", FunManager::defineMacro);
            reg("let", applyArgs -> let(applyArgs.getExp(), applyArgs.getEnv(), applyArgs::eval));
            reg("set!", applyArgs -> set(applyArgs.getExp(), applyArgs.getEnv(), applyArgs::eval));
            reg("apply", applyArgs -> apply0(applyArgs, applyArgs.getExp(), applyArgs.getEnv()));
            regBooleanFun();
            regCons();
            regSymbolsFun();
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
                    if (args.carSymbols().equals(Symbols.of("."))) {
                        loopBind(env, args.cdr(), val.data().size() == 1 && IS_EXP.test(val.car()) ? val : ConsMarker.markList(val));
                    } else {
                        env.setEnv(args.carSymbols(), val.car());
                        if (!args.cdr().isEmpty()) {
                            loopBind(env, args.cdr(), val.cdr());
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
            Object o = null;
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
                return null;
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
            while (Objects.nonNull(env.parent()) && Objects.nonNull(env.parent().parent())) {
                env = env.parent();
            }
            validateTrue(env.noContains(cdr.carSymbols()), "Do not repeat the definition " + cdr.carSymbols());
            env.setEnv(cdr.carSymbols(), val);
            return null;
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
            return null;
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
            return null;
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
                return Stream.of(ts).allMatch(FunManager::toBoolean) ? ts[ts.length - 1] : false;
            });
            reg("or", applyArgs -> Stream.of(applyArgs.args()).filter(FunManager::toBoolean).findFirst().orElse(false));
            reg("not", applyArgs -> {
                Object[] ts = applyArgs.args();
                validateTrue(ts.length == 1, applyArgs.getExp() + "not args only one");
                return !toBoolean(ts[0]);
            });
            reg("<", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x < (Integer) y : x.toString().length() < y.toString().length()));
            reg("<=", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x <= (Integer) y : x.toString().length() <= y.toString().length()));
            reg("=", applyArgs -> predicate(applyArgs, Object::equals));
            reg("!=", applyArgs -> predicate(applyArgs, (x, y) -> !x.equals(y)));
            reg(">", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x > (Integer) y : x.toString().length() > y.toString().length()));
            reg(">=", applyArgs -> predicate(applyArgs, (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x >= (Integer) y : x.toString().length() >= y.toString().length()));
        }

        private static void regCons() {
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
            reg("list-ref", applyArgs -> {
                Object[] x = applyArgs.args();
                return ((Cons) x[0]).list().get((Integer) x[1]);
            });
            reg("list-tail", applyArgs -> {
                Object[] x = applyArgs.args();
                Cons cons = (Cons) x[0];
                return ConsMarker.markList(cons.list().subList((Integer) x[1], cons.list().size()));
            });
            reg("null?", applyArgs -> {
                Object r = applyArgs.eval(applyArgs.getExp());
                return warp(IS_EXP.test(r) && ((Cons) r).isEmpty());
            });
            reg("cons->arraylist", applyArgs -> warp(applyArgs.args()[0]));

        }

        private static void regSymbolsFun() {
            reg("symbol?", applyArgs -> warp(applyArgs.eval(applyArgs.getExp()) instanceof Symbols));
            reg("eqv?", applyArgs -> warp(applyArgs.getExp().car().equals(applyArgs.getExp().cdr().car())));
            reg("gensym", applyArgs -> Symbols.of("gen-" + new Random().nextInt(1024)));
        }

        private static Object predicate(ApplyArgs applyArgs, BiPredicate<Object, Object> predicates) {
            Object[] objs = applyArgs.args();
            validateTrue(objs.length > 1, applyArgs.getExp() + " args qty > 1 ");
            Object o = applyArgs.eval(objs[0], applyArgs.getExp(), applyArgs.getEnv());
            for (int i = 1; i < objs.length; i++) {
                Object o1 = applyArgs.eval(objs[i], applyArgs.getExp(), applyArgs.getEnv());
                boolean b = predicates.test(o, o1);
                if (!b) {
                    return false;
                }
                o = o1;
            }
            return true;
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