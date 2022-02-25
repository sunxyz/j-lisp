# j-lisp

> use java impl lisp (scheme) interpreter

## Implemented functions
- [x] atom: numbers booleans symbols , strings
- [x] Four arithmetic
- [x] load
- [x] lambda
- [x] quote
- [x] display
- [x] newline
- [x] begin
- [x] define
- [x] define-macro
- [x] let
- [x] set!
- [x] apply
- [x] if
- [x] cond
- [x] boolean: and or not < <=  = != > >=
- [x] cons list car cdr ...
- [x] symbol? eqv? gensym

**dialect**
- [x] lambda-lep => lambda-lazy-eval-params
## use
**simple**
```java
System.out.println(eval("(+ 1 1)"));
=> 2
System.out.println(eval("(+ 1 2 (+ 3 4))"));
=> 10
System.out.println(eval("((define a (list 1 2 3 4 5)) a)"));       
=> (1 2 3 4 5)
```
**load**
```java
System.out.println(eval("(load 'alias.lisp' 'test.lisp')")); 
```
- alias.lisp
```
(
    (define λ lambda)
    (define ` quote)
)
```
- test.lisp
```
(
    (define cons (lambda (x y) (lambda(g)(g x y))))
    (define car (lambda (f) (f (lambda(x y)(x)))))
    (define cdr (lambda (f) (f (lambda(x y)(y)))))
)
```
**define-macro**
- fluid-let.lisp
```
(
    (define map (lambda (g x . y)
        (cons
            (g (car x) (car y))
            (if (null? (cdr x))
                (quote ())
                (map g (cdr x) (if (null? y) (quote ()) (cdr y)))))))
                
    (define x 3)
    
    (define y 5)
    
    (define d 5)
    
    (define-macro fluid-let
      (lambda (xexe . body)
        (let ((xx (map car xexe))
              (ee (map (lambda (x) (car (cdr x))) xexe))
              (old-xx (map (lambda (ig) (gensym)) xexe))
              (result (gensym)))
          (`(let ,(map (lambda (old-x x) (`(, old-x , x)))
                      old-xx xx)
             ,@ (map (lambda (x e)
                      (`(set! , x , e)))
                    xx ee)
             (let ((, result (begin ,@ body)))
               ,@(map (lambda (x old-x)
                        (`(set! , x , old-x)))
                      xx old-xx)
               , result))))))
               
    (fluid-let ((x 9) (y (+ y 1)) (d 8))
      (+ x y))
)
```
- when.lisp
```
(
    (define-macro when
      (lambda (test . branch)
        (` (IF , test
             (BEGIN , branch)))))
)
```
- when2.lisp
```
(
    (define-macro when2
      (lambda (test . branch)
        (list (` if) test
             (cons (` begin) branch))))
)
```
- when3.lisp
```
(
    (lambda (test . branch)
        (list (` if) test
            (cons (` begin) branch)))
    (` #t)  (` (+ 1 2))
)
```

```
=> (if #t (begin (+ 1 2)))
```
```
(apply (if #t (begin (+ 1 2))))
```
```
=> 3
```
```
(apply 
    (
        (lambda (test . branch)
            (list (` if) test
                (cons (` begin) branch)))
         (` #t)  (` (+ 1 2)))
)
```
```
=> 3
```
- when4.lisp
```
(apply
    (lambda (test . branch)
        (list (` if) test
            (cons (` begin) branch)))
    (` (#t  (+ 1 2))))
```
```
=> (if #t (begin (+ 1 2)))
```
**dialect** 
- when5: lambda-lep
```
(
    (define when5
      (lambda-lep (test . branch)
        (apply (` (IF , test
             (BEGIN , branch))))))
)
```
```
(when5 (< 1 2) (display 1))
=> 1
```
## core-code

**JLispInterpreter3**

```
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
        Object v = IS_EXP.test(exp.car()) ? eval(exp.carCons(), env) : (IS_SYMBOLS.test(exp.car()) ? env.env(exp.carSymbols()).orElseThrow(() -> new IllegalArgumentException(exp.parent() + ": " + exp.carSymbols() + " not define")) : exp.car());
        return CAN_APPLY.test(exp, v) ? apply(v, exp.cdr(), env) : (exp.cdr().isEmpty() ? v : eval(exp.cdr(), env));
    }

    private static Object eval(Object o, Cons exp, Env env) {
        return IS_EXP.test(o) ? eval((Cons) o, env) : (IS_SYMBOLS.test(o) ? eval(ConsMarker.markSubExp(exp, o), env) : o);
    }

    private static Object apply(Object v, Cons cdr, Env env, Object... args) {
        return ((Function<ApplyArgs, Object>) v).apply(ApplyArgs.of(cdr, env, () -> cdr.data().stream().map(o -> eval(o, cdr, env)).toArray(), args.length > 0 ? args : null));
    }

    @AllArgsConstructor(staticName = "of")
    @Getter
    public static class ApplyArgs extends EvalAndApplyProxy {
        final Cons exp;
        final Env env;
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
            return ConsMarker.markList(args());
        }
    }
}
```

**why 3?** 
- Third coded
- Three begets all things

**How to expand functions ？**
- simple reg + - * /
```
static class FunManager {
    private static final Map<String, Function<ApplyArgs, Object>> FUNCTIONAL = new ConcurrentHashMap<>();

    static {
        reg("+", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce(Integer::sum).orElse(null));
        reg("-", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a - b).orElse(null));
        reg("*", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a * b).orElse(null));
        reg("/", applyArgs -> toIntStream(applyArgs.getLazyArgs()).reduce((a, b) -> a / b).orElse(null));
    }
    
    private static Stream<Integer> toIntStream(Supplier<Object[]> supplierObjs) {
        return Stream.of(supplierObjs.get()).map(Object::toString).map(Integer::valueOf).collect(Collectors.toList()).stream();
    }
}
```
