package org.yangrd.lab.lisp;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.atom.Atom;
import org.yangrd.lab.lisp.atom.Booleans;
import org.yangrd.lab.lisp.atom.Strings;
import org.yangrd.lab.lisp.atom.Symbols;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class JLispInterpreter3 {

    private static final Predicate<Object> IS_EXP = o -> o instanceof Cons ;

    private static final Predicate<Object> IS_SYMBOLS = o -> o instanceof Symbols;

    private static final Predicate<Object> IS_FUN = o -> o instanceof Function;

    private static final Predicate<Object> IS_ATOM = o -> o instanceof Atom;

    private static final BiPredicate<Cons, Object> CAN_APPLY = (exp, v) -> IS_FUN.test(v) && !exp.isSubExp();

    public static void main(String[] args) {
        log.debug("=>{}", eval("((load 'lib.lisp' 'alias.lisp') ((r(x)(* x x))(cdr (cons 1 2))))"));
    }

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

    private static Object apply(Object v, Cons cdr, Env env) {
        return ((Function<ApplyArgs, Object>) v).apply(ApplyArgs.of(cdr, env, () -> cdr.data().stream().map(o -> getAtom(o, cdr, env)).toArray(), JLispInterpreter3::eval, JLispInterpreter3::eval));
    }

    private static Object getAtom(Object o, Cons exp, Env env) {
        if (IS_EXP.test(o)) {
            return eval((Cons) o, env);
        } else if (IS_SYMBOLS.test(o)) {
            return eval(markSubExp(exp,o), env);
        } else {
            return o;
        }
    }

    private static Cons markSubExp(Cons parent, Object obj) {
        return Cons.of(Collections.singletonList(obj), parent, Cons.ConsType.SUB_EXP);
    }

    @Value(staticConstructor = "of")
    public static class ApplyArgs {
        Cons exp;
        Env env;
        Supplier<Object[]> lazyArgs;
        BiFunction<Cons, Env, Object> eval;
        BiFunction<String, Env, Object> evalStr;
    }

    static class FunManager {
        private static final Map<String, Function<ApplyArgs, Object>> FUNCTIONAL = new ConcurrentHashMap<>();

        static {
            reg("+", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce(Integer::sum).orElse(null));
            reg("-", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a - b).orElse(null));
            reg("*", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a * b).orElse(null));
            reg("/", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a / b).orElse(null));
            reg("load", applyArgs -> load(applyArgs.getEnv(), applyArgs.getLazyArgs().get(), applyArgs.getEvalStr()));
            reg("lambda", applyArgs -> lambda(applyArgs.getExp(), applyArgs.getEnv()));
            reg("quote", applyArgs -> quote(applyArgs.getExp(),applyArgs.getEnv()));
            reg("display", applyArgs -> { System.out.print(applyArgs.getEval().apply(applyArgs.getExp(), applyArgs.getEnv()));return null; });
            reg("newline", applyArgs -> { System.out.println();return null;});
            reg("begin", applyArgs ->  applyArgs.getEval().apply(applyArgs.getExp(),applyArgs.getEnv()) );
            reg("define", FunManager::define);
            reg("define-macro", FunManager::defineMacro);
            reg("let", applyArgs -> let(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            reg("set!", applyArgs -> set(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            reg("apply", applyArgs -> apply0(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            regBooleanFun();
            regCons();
            regSymbolsFun();
            reg("if", applyArgs -> if0(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            reg("cond", applyArgs -> cond(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
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
                    return applyArgs.getEval().apply(body, env0);
                }

                private void bindEnv(ApplyArgs applyArgs, Env env) {
                    //参数值
                    Object[] x = applyArgs.getLazyArgs().get();
                    // 看是否有可变参数
                    List<Object> args = cdr.carCons().list();
                    int argsSize = args.size();
                    boolean indefiniteLengthArgsFlag = argsSize > 1 && ((Symbols) args.get(argsSize - 2)).getVal().equals(".");
                    validateTrue((indefiniteLengthArgsFlag || args.size() == x.length)&&x.length>=argsSize-1, cdr.parent() + "参数不一致");
                    int i = 0;
                    if (indefiniteLengthArgsFlag) {
                        for (int j = 0; j < argsSize - 2; j++) {
                            env.setEnv(((Symbols) args.get(i)), x[i]);
                            i++;
                        }
                        env.setEnv(((Symbols) args.get(argsSize - 1)), markList(Arrays.copyOfRange(x, argsSize - 2, x.length)));
                    } else {
                        for (Object argName : args) {
                            env.setEnv(((Symbols) argName), x[i]);
                            i++;
                        }
                    }
                }

                @Override
                public String toString() {
                    return "<procedure>";
                }
            };
        }

        private static Object quote(Cons cdr,Env env) {
            // 支持 ， ,@
            //,紧跟的表达式需要eval  ,@ 紧跟的表达式需要先 splice 在 eval
            Object car = cdr.car();
            if(IS_EXP.test(car)){
                Symbols s0 = Symbols.of(",");
                Symbols s1 = Symbols.of(",@");
                Cons cons = cdr.carCons();
                if(cons.data().stream().anyMatch(o->o.equals(s0)||o.equals(s1))){
                    Cons exps = markExp();
                    Object pre = null;
                    for (Object o : cons) {
                        if(s0.equals(pre)){
                            exps.add(getAtom(o, cdr,env));
                        }else if(s1.equals(pre)){
                            Object obj = getAtom(o, cdr, env);
                            validateTrue(IS_EXP.test(obj),s1+" The return value of the last element needs to be an list");
                            ((Cons)obj).data().forEach(exps::add);
                        }else if (!s0.equals(o)&&!s1.equals(o)){
                            exps.add(o);
                        }
                        pre = o;
                    }
                    return exps;
                }else{
                    return car;
                }
            }else {
                return car;
            }
        }

        private static Object load(Env env, Object[] args, BiFunction<String, Env, Object> eval) {
            Arrays.asList(args).forEach(d -> {
                validateTrue(d instanceof Strings, d + " type error");
                String file = ((Strings) d).getVal();
                String str = FileUtils.readFile(file);
                eval.apply(str, env);
            });
            return null;
        }

        private static Object if0(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            Object car = cdr.car();
            boolean isTure = toBoolean(IS_EXP.test(car)?eval.apply(cdr.carCons(), env):car);
            if (isTure) {
                Object then = cdr.cdr().car();
                return getAtom(then, cdr.cdr(), env);
            } else {
                if (cdr.cdr().cdr().data().size() > 0) {
                    return eval.apply(cdr.cdr().cdr(), env);
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
            Object val = applyArgs.eval.apply(cdr.cdr(), applyArgs.getEnv());
            validateTrue(applyArgs.getEnv().noContains(cdr.carSymbols()), "Do not repeat the definition " + cdr.carSymbols());
            applyArgs.getEnv().setEnv(cdr.carSymbols(), val);
            return null;
        }

        private static Object defineMacro(ApplyArgs applyArgs) {
            Cons cdr = applyArgs.getExp();
            validateTrue(applyArgs.getEnv().noContains(cdr.carSymbols()), "Do not repeat the definition " + cdr.carSymbols());
            Function<ApplyArgs,Object> applyFun = (applyArgs1)->{
                Cons cons = markList(Symbols.of("apply"), cdr.cdr().car(),markQuote(applyArgs1.getExp().list().toArray()));
                Object apply = applyArgs1.getEval().apply(cons, applyArgs1.getEnv());
                System.out.println(apply);
                return getAtom(apply,cdr,applyArgs1.getEnv());
            };
            applyArgs.getEnv().setEnv(cdr.carSymbols(), applyFun);
            return null;
        }

        private static Object let(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            Object car0 = cdr.car();
            validateTrue(car0 instanceof Cons && cdr.data().size() == 2, "please check" + car0);
            Env env0 = Env.newInstance(env);
            Cons car = cdr.carCons();
            Cons body = cdr.cdr().carCons();
            for (Object con : car) {
                validateTrue(IS_EXP.test(con), "please check" + con);
                Cons item = (Cons) con;
                Symbols var = item.carSymbols();
                validateTrue(env0.noContains(var), "Do not repeat the let " + var);
                env0.setEnv(var, eval.apply(item.cdr(), env));
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

        private static Object apply0(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            // 此处后期需重构
            Object f = cdr.car();
            f = getAtom(getAtom(f, cdr, env),cdr,env);
            if (IS_FUN.test(f)) {
                return apply(f, flatMap(cdr.cdr(), env, eval), env);
            } else {
                return f;
//                throw new IllegalArgumentException("apply " + cdr);
            }
        }

        private static Cons flatMap(Cons args, Env env,BiFunction<Cons, Env, Object> eval){
            List<Object> list = args.list();
            Cons exp = markExp(list.subList(0, list.size() - 1).toArray());
            Object last = list.get(list.size() - 1);
            if(IS_EXP.test(last)){
                Cons last1 = (Cons) last;
                Object r = eval.apply(last1, env);
                if(IS_EXP.test(r)/*判断是列表*/){
                    Cons r1 = (Cons) r;
                    // 此处最好比较的是全局的
                    boolean quote =  IS_SYMBOLS.test(r1.parent().car())&&env.env(r1.parent().carSymbols()).orElseThrow(RuntimeException::new).equals(FUNCTIONAL.get("quote"));
                    r1.data().stream().map(o->IS_EXP.test(o)&&quote?markQuote(((Cons)o).data().toArray()):o).forEach(exp::add);
                }else{
                    exp.add(last);
                }
            }else{
                exp.add(last);
            }
            return exp;
        }

        private static void validateTrue(boolean flag, String err) {
            if (!flag) {
                throw new IllegalArgumentException(err);
            }
        }

        private static void regBooleanFun() {
            reg("and", applyArgs -> {
                Object[] ts = applyArgs.getLazyArgs().get();
                return Stream.of(ts).allMatch(FunManager::toBoolean) ? ts[ts.length - 1] : false;
            });
            reg("or", applyArgs -> Stream.of(applyArgs.getLazyArgs().get()).filter(FunManager::toBoolean).findFirst().orElse(false));
            reg("not", applyArgs -> {
                Object[] ts = applyArgs.getLazyArgs().get();
                validateTrue(ts.length == 1, applyArgs.getExp() + "not args only one");
                return !toBoolean(ts[0]);
            });
            reg("<", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x < (Integer) y : x.toString().length() < y.toString().length()));
            reg("<=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x <= (Integer) y : x.toString().length() <= y.toString().length()));
            reg("=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), Object::equals));
            reg("!=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> !x.equals(y)));
            reg(">", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x > (Integer) y : x.toString().length() > y.toString().length()));
            reg(">=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x >= (Integer) y : x.toString().length() >= y.toString().length()));
        }

        private static void regCons() {
            reg("cons", applyArgs -> markList(Arrays.copyOf(applyArgs.getLazyArgs().get(), 2)));
            reg("car", applyArgs -> ((Cons) (applyArgs.getLazyArgs().get()[0])).car());
            reg("cdr", applyArgs -> ((Cons) (applyArgs.getLazyArgs().get()[0])).cdr().car());
            reg("set-car!", applyArgs -> {
                Object[] x = applyArgs.getLazyArgs().get();
                ((Cons) x[0]).list().set(0, x[1]);
                return null;
            });
            reg("set-cdr!", applyArgs -> {
                Object[] x = applyArgs.getLazyArgs().get();
                ((Cons) x[0]).list().set(1, x[1]);
                return null;
            });
            reg("list", applyArgs -> markList(applyArgs.getLazyArgs().get()));
            reg("list-ref", applyArgs -> {
                Object[] x = applyArgs.getLazyArgs().get();
                return ((Cons) x[0]).list().get((Integer) x[1]);
            });
            reg("list-tail", applyArgs -> {
                Object[] x = applyArgs.getLazyArgs().get();
                Cons cons = (Cons) x[0];
                return markList(cons.list().subList((Integer) x[1], cons.list().size()));
            });
        }

        private static void regSymbolsFun(){
            reg("symbol?", applyArgs -> warp(applyArgs.getEval().apply(applyArgs.getExp(), applyArgs.getEnv()) instanceof Symbols));
            reg("eqv?", applyArgs -> warp(applyArgs.getExp().car().equals(applyArgs.getExp().cdr().car())));
        }

        private static Object predicate(Cons exp, Supplier<Object[]> lazyArgs, BiPredicate<Object, Object> predicates) {
            Object[] objs = lazyArgs.get();
            validateTrue(objs.length > 1, exp + " args qty > 1 ");
            Object o = objs[0];
            for (int i = 1; i < objs.length; i++) {
                Object o1 = objs[i];
                boolean b = predicates.test(o, o1);
                if (!b) {
                    return b;
                }
                o = o1;
            }
            return true;
        }

        private static boolean toBoolean(Object o) {
            if (o instanceof Boolean) {
                return (Boolean) o;
            }else if(o instanceof Booleans){
                return ((Booleans) o).getVal();
            } else {
                return !o.equals(0);
            }
        }

        private static Object warp(Object o){
            if(o instanceof String){
                return Strings.of((String) o);
            }if (o instanceof Boolean){
                return Booleans.of((Boolean) o);
            }
            return o;
        }

        private static Stream<Integer> toIntStream(Supplier<Object[]> supplierObjs) {
            return Stream.of(supplierObjs.get()).map(Object::toString).map(Integer::valueOf).collect(Collectors.toList()).stream();
        }

        private static Cons markList(Object... data) {
            return Cons.of(new ArrayList<>(Arrays.asList(data)), null, Cons.ConsType.LIST);
        }

        private static Cons markExp(Object... data) {
            return Cons.of(new ArrayList<>(Arrays.asList(data)), null, Cons.ConsType.EXP);
        }

        private static Cons markExp(Cons parent, Object... data) {
            return Cons.of(new ArrayList<>(Arrays.asList(data)), parent, Cons.ConsType.EXP);
        }

        private static Cons markQuote(Object... data){
            Cons quote = Cons.of(new ArrayList<>(Collections.singletonList(Symbols.of("quote"))), null, Cons.ConsType.QUOTE);
            if(data.length>0){
                quote.add(markExp(quote, data));
            }
            return quote;
        }
    }
}
