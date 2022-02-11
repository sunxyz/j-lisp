package org.yangrd.lab.lisp;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.atom.Strings;
import org.yangrd.lab.lisp.atom.Symbols;

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

    private static final Predicate<Object> IS_ATOM = o -> o instanceof Number || o instanceof Strings || o instanceof Boolean;

    private static final BiPredicate<Cons, Object> CAN_APPLY = (exp, v) -> IS_FUN.test(v) && exp.isExp();

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
            return eval(toSubCons(o, exp), env);
        } else {
            return o;
        }
    }

    private static Cons toSubCons(Object obj, Cons parent) {
        return Cons.of(Collections.singletonList(obj),parent, false);
    }

    @Value(staticConstructor = "of")
    public static class ApplyArgs {
        Cons exp;
        Env env;
        Supplier<Object[]> lazyArgs;
        BiFunction<Cons, Env, Object> eval;
        BiFunction<String, Env, Object> evalStr;
    }

    static  class FunManager{
        private static final Map<String, Function<ApplyArgs, Object>> FUNCTIONAL = new ConcurrentHashMap<>();

        static {
            reg("+", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce(Integer::sum).orElse(null));
            reg("-", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a - b).orElse(null));
            reg("*", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a * b).orElse(null));
            reg("/", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a / b).orElse(null));
            reg("load", applyArgs -> load(applyArgs.getEnv(), applyArgs.getLazyArgs().get(), applyArgs.getEvalStr()));
            reg("lambda", applyArgs -> lambda(applyArgs.getExp(), applyArgs.getEnv()));
            reg("quote", applyArgs -> quote(applyArgs.getExp()));
            reg("define", FunManager::define);
            reg("let", applyArgs -> let(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            reg("set!", applyArgs -> set(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            reg("apply", applyArgs -> apply0(applyArgs.getExp(), applyArgs.getEnv()));
            regBooleanFun();
            regCons();
//            reg("if", applyArgs -> if0(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
            reg("cond", applyArgs -> cond(applyArgs.getExp(), applyArgs.getEnv(), applyArgs.getEval()));
        }

        private static void reg(String optKey, Function<ApplyArgs, Object> opt) {
            FUNCTIONAL.put(optKey, opt);
        }

        private static Function<ApplyArgs, Object> lambda(Cons cdr, Env env) {
            return (applyArgs) -> {
                Object[] x = applyArgs.getLazyArgs().get();
                Cons args = cdr.carCons();
                Cons body = cdr.cdr();
                validateTrue(args.data().size() == x.length, cdr.parent()+"参数不一致");
                Env env0 = Env.newInstance(env);
                int i = 0;
                for (Object argName : args) {
                    env0.setEnv(((Symbols) argName), x[i]);
                    i++;
                }
                return applyArgs.getEval().apply(body, env0);
            };
        }

        private static Object quote(Cons cdr) {
            return cdr.carCons();
        }

        private static Object load(Env env, Object[] args,  BiFunction<String, Env, Object> eval){
            Arrays.asList(args).forEach(d->{
                validateTrue(d instanceof Strings,d+" type error");
                String file = ((Strings) d).getVal();
                String str = FileUtils.readFile(file);
                eval.apply(str, env);
            });
            return null;
        }

        private static Object if0(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval) {
            Cons car = cdr.carCons();
            if (isaBoolean(eval.apply(car, env))) {
                Object then = cdr.cdr().car();
                return getAtom(then, cdr.cdr(), env);
            } else {
                if (cdr.cdr().cdr().data().size() > 0) {
                    return eval.apply(cdr.cdr().cdr(), env);
                }
                return null;
            }
        }

        private static Object cond(Cons cdr, Env env, BiFunction<Cons, Env, Object> eval){
            Cons car = cdr.carCons();
            Cons predicateExp = car.carCons();
            Cons body = car.cdr();
            if(isaBoolean(eval.apply(predicateExp, env))){
                return eval.apply(body, env);
            }else{
                Cons elseCdr = cdr.cdr();
                if(elseCdr.data().size()==1){
                    // 去掉括號
                    while (IS_EXP.test(elseCdr.car())&&elseCdr.data().size()==1){
                        elseCdr = elseCdr.carCons();
                    }
                    validateTrue(IS_SYMBOLS.test(elseCdr.car())&&elseCdr.carSymbols().getVal().equals("else"),"cond last item not else key");
                    return  eval.apply(elseCdr.cdr(), env);
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

        private static Object set(Cons cdr,Env env, BiFunction<Cons, Env, Object> eval) {
            Symbols var = cdr.carSymbols();
            Object val = eval.apply(cdr.cdr(),env);
            validateTrue(env.env(var).isPresent(), " not definition set! error " + var);
            Env envParent = env;
            while (envParent.noContains(var)) {
                envParent = envParent.parent();
            }
            envParent.setEnv(var, val);
            return null;
        }

        private static Object apply0(Cons cdr, Env env) {
            Object f = cdr.car();
            f = IS_EXP.test(f)? eval((Cons) f,env):f;
            f = getAtom(f, cdr, env);
            if (IS_FUN.test(f)) {
                return apply(f, cdr.cdr(), env);
            } else {
                throw new IllegalArgumentException("apply " + cdr);
            }
        }

        private static void validateTrue(boolean flag, String err) {
            if (!flag) {
                throw new IllegalArgumentException(err);
            }
        }

        private static void regBooleanFun() {
            reg("and", applyArgs -> {
                Object[] ts = applyArgs.getLazyArgs().get();
                return Stream.of(ts).allMatch(FunManager::isaBoolean) ? ts[ts.length - 1] : false;
            });
            reg("or", applyArgs -> Stream.of(applyArgs.getLazyArgs().get()).filter(FunManager::isaBoolean).findFirst().orElse(false));
            reg("not", applyArgs -> {
                Object[] ts = applyArgs.getLazyArgs().get();
                validateTrue(ts.length == 1, applyArgs.getExp() + "not args only one");
                return !isaBoolean(ts[0]);
            });
            reg("<", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x < (Integer) y : x.toString().length() < y.toString().length()));
            reg("<=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x <= (Integer) y : x.toString().length() <= y.toString().length()));
            reg("=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), Object::equals));
            reg("!=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> !x.equals(y)));
            reg(">", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x > (Integer) y : x.toString().length() > y.toString().length()));
            reg(">=", applyArgs -> predicate(applyArgs.getExp(), applyArgs.getLazyArgs(), (x, y) -> (x instanceof Integer && y instanceof Integer) ? (Integer) x >= (Integer) y : x.toString().length() >= y.toString().length()));
        }

        private static void regCons(){
            reg("cons", applyArgs ->  {
                Cons cons = Cons.newInstance(null);
                Arrays.asList( applyArgs.getLazyArgs().get()).subList(0,2).forEach(cons::add);
                return cons;
            });
            reg("car", applyArgs ->  getAtom(((Cons)(applyArgs.getLazyArgs().get()[0])).car(), applyArgs.getExp(),applyArgs.getEnv()));
            reg("cdr", applyArgs -> getAtom(((Cons)(applyArgs.getLazyArgs().get()[0])).cdr().car(), applyArgs.getExp(),applyArgs.getEnv()));
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
            reg("list", applyArgs -> {
                Cons cons = Cons.newInstance(null);
                Arrays.asList( applyArgs.getLazyArgs().get()).forEach(cons::add);
                return cons;
            });
            reg("list-ref", applyArgs -> {
                Object[] x = applyArgs.getLazyArgs().get();
                return getAtom(((Cons) x[0]).list().get((Integer)x[1]), applyArgs.getExp(), applyArgs.getEnv());
            });
            reg("list-tail", applyArgs -> {
                Object[] x = applyArgs.getLazyArgs().get();
                Cons cons = (Cons) x[0];
                return  Cons.of(cons.list().subList((Integer)x[1], cons.list().size()),null,false);
            });
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

        private static boolean isaBoolean(Object o) {
            if (o instanceof Boolean) {
                return (Boolean) o;
            } else {
                return !o.equals(0);
            }
        }

        private static Stream<Integer> toIntStream(Supplier<Object[]> supplierObjs) {
            List<Object> l = listArgs(supplierObjs.get());
            return l.stream().map(Object::toString).map(Integer::valueOf).collect(Collectors.toList()).stream();
        }

        private static List<Object> listArgs(Object[] ts) {
            List<Object> l = new ArrayList<>();
            for (int i = 0; i < ts.length; i++) {
                if(i<ts.length-1){
                    l.add(ts[i]);
                }else{
                    if(IS_EXP.test(ts[i])){
                        l.addAll(((Cons)ts[i]).data());
                    }else{
                        l.add(ts[i]);
                    }
                }
            }
            return l;
        }
    }
}
