package org.yangrd.lab;

import static org.yangrd.lab.lisp.JLispInterpreter3.eval;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {
//        System.out.println( eval("((define a (lambda () 'hello  world')) (a))") );
//        System.out.println( eval("(+ 1 2 3 4 (+ 5 6) (+ 7 8 (+ 9 0)))") );
//        System.out.println( eval("(let (" +
//                "(cons  (lambda (x y) (lambda (g) (g x y)))) " +
//                "(car (lambda (f) (f (lambda(x y)(x))))) " +
//                "(cdr (lambda (f) (f (lambda(x y)(y)))))" +
//                ")" +
//                "(let (" +
//                "(if (lambda (p then_v else_v) ((or (and p car) cdr) (cons then_v else_v))))" +
//                ")(if (< 5 6) false 1)))") );
//        System.out.println(eval("((load 'lib.lisp' 'alias.lisp')(define fact (lambda (n)" +
//                "  (if (= n 1)" +
//                "      1" +
//                "      ( lambda () ((* n ((fact (- n 1))))))))) (fact 10))"));
//        System.out.println(eval("((load 'lib.lisp' 'alias.lisp')(define fact (lambda (n)" +
//                "  (if (= n 1)" +
//                "      1" +
//                "     (` (* n ((fact (- n 1)))))))) (fact 5))"));
//        System.out.println(eval("((define c  3)(define b (cons c 8)) (set-cdr! b 3) (let ((c 5))((car b))))"));
//        System.out.println(eval("((define c  3)(define b ((list c 8 7 8 9 10)))  (apply + b))"));
//        System.out.println(eval("((define c  3)(define b (  (list c ((lambda () (6))) 7 8 9 10))) (set! c 10)  (b))") +"  "+eval("((define c  3)(define b (quote  ( c (((lambda () (6)))) (list 1 2) 7 8 9 10))) (set! c 10)  (list-ref b 2))"));
//        System.out.println(eval("((load 'lib.lisp' 'alias.lisp')((`(11))))"));
//        System.out.println(eval("((define counter 0)\n" +
//                "\n" +
//                "(define bump-counter\n" +
//                "  (lambda ()\n" +
//                "    (set! counter (+ counter 1))\n" +
//                "    counter)) (bump-counter) (bump-counter))"));
//        System.out.println(eval("((define return (lambda  (x) x)) (define k+ (lambda (a b k) (k (+ a b)))) (define k* (lambda (a b k) (k (* a b)))) (k+ 1 2 (lambda (x) (k* x 3 return))))"));
// test- macro
//        System.out.println(eval("(   ((define-macro when (lambda (p then . else)  (list (quote if) p then else))) ((when (> 1 2) (+ 1 2) 7 8 (9 7 8) ))))"));
//        System.out.println(eval("(apply (apply (lambda (p then else)  (list (quote if) p then (quote else) else)) (quote (< 1 2)) (quote(+ 1 2)) 7))"));
//        System.out.println(eval("( ( (load 'alias.lisp') (apply (λ (p then . else)  (list (` if) p then else)) (` ((> 10 9) (+ 1 2) 7 8 (9 7 8))))))"));
//        System.out.println(eval("(  (  (lambda (p then . else)  (list (quote if) p then else)) (quote (> 1 2) ) (quote (+ 1 2))  (quote 7) (quote 8) (quote (9 7 8)))) )"));
//        System.out.println(eval("( (  (apply (lambda (p then . else)  (list (quote if) p then else)) (quote (< 1 0)) (quote (+ 1 8))  (quote (if (< 7 8) 8 9))) )))"));
//        System.out.println(eval("(apply (lambda (p then . else) (list (quote if) p then else)) (quote ((> 1 2) (+ 1 2) 7 8 (9 7 8))))"));
//        System.out.println(eval("((load 'lib.lisp')(unless (< 5 8) (1 2 3 4) 4 5 6))"));
//        System.out.println(eval("(apply (lambda (p then . else) (list (quote if) p then else)) (quote ((> 1 2) (+ 1 2) 7 8 (9 7 8))))"));

//        System.out.println(eval("(   ((define-macro when (lambda (p then . else)  (quote (if , p , then , else)))) ((when (> 1 2) (+ 1 2) 7 8 (9 7 8) ))))"));
       eval("(load 'alias.lisp' 'lib.lisp' 'test-1.lisp')");
//        System.out.println(eval("((define a (list 1 2 3 4 5)) a)"));
    }

//    public static void main(String[] args) {
////          log.debug("{}", eval("(1 2 3 4 5 6)"));
////        log.debug("{}", eval("(+ 1 2 3 4 (+ 5 6) (+ 7 8 (+ 9 0)))"));
////        log.debug("{}", eval("(+ 1 2 3 4 (- 9 3))"));
////        log.debug("{}", eval("((lambda (x) (+ 5 x)) 5)"));
////        log.debug("{}", eval("((lambda (x) (+ 5 x)) ((lambda (y) (* y y)) 5))"));
////        log.debug("{}", eval("(( (lambda(x)(lambda(o)(o 5 x))) ((lambda(y)(* y y)) 5) ) +)"));
////        log.debug("=> {}", eval("((define a (lambda (x) (lambda (y) (+ x y)))) (define b 9) ((a b) 6))"));
////        log.debug("=> {}", eval("(let ((a 5) (b 5) (g (lambda (x y) (+ x y)))) (g a b))"));
////        log.debug("=> {}", eval("((lambda (x) (cond ((< 5 x) 1) ((> 5 x) -1) (else 0) )) 6)"));
////        log.debug("=> {}", eval("(let ((a 5) (g (lambda (x) (cond ((< 5 x) 1) ((> 5 x) -1) (else 0) )))) (g a))"));
////        log.debug("=> {}", eval("(let ((a 5) (b 6)) ((set! a 6) (+ a b)))"));
////        log.debug("=> {}", eval("(let ((x 2) (y 3) )((define > <)(if (> x y)(+ x y)(- x y)))"));
//        log.debug("=> {}", eval("((define fee (lambda (age)\n" +
//                "  (cond" +
//                "   ((or (<= age 3) (>= age 65)) 0)\n" +
//                "   ((<= 4 age 6) 1)\n" +
//                "   ((<= 7 age 6) 2)\n" +
//                "   ((<= 13 age 15) 3)\n" +
//                "   ((<= 16 age 18) 4)\n" +
//                "   (else 5)))) (fee 8))"));
////        log.debug("=> {}", eval("((define a (lambda (x) (lambda (y) (+ x y)))) (define b 9) (apply (a b) 6))"));
////                log.debug("=> {}", eval("(let ((a 5) (g (lambda (x) (cond ((< 5 x) 1) ((> 5 x) -1) (else 0) )))) (apply g a))"));
////        log.debug("=> {}", eval("(let ((cons (lambda (x y) (lambda (g) (g x y)))) (car (lambda (f) (f (lambda (x y) (x))))) (cdr (lambda (f) (f (lambda (x y) (y)))))) (cdr (cons 1 2))"));
//        log.debug("=> {}", eval("(let ((cons (lambda (x y) (lambda (g) (g x y (lambda (a) (set! x a)) (lambda (a) (set! y a)))))) (car (lambda (f) (f (lambda (x y sx sy) (x))))) (cdr (lambda (f) (f (lambda (x y sx sy) (y))))) (set-car! (lambda (f a) (f (lambda (x y sx sy) ((sx a) f))))) (set-cdr! (lambda (f a) (f (lambda (x y sx sy) ((sy a) f)))))) (cdr (set-cdr! (cons 1 2) 3)))"));
//
//    }

//    public static void main(String[] args) {
//        log.debug("{}", (parse("(1 2 3 4 5 6 7 (8 9 10) 11 12 (13 (14 15)) 16 true 's 5 8' '456  ' a b c (7 8 9))")));
//        log.debug("{}", parse("(let ((cons (lambda (x y) (lambda (g) (g x y (lambda (a) (set! x a)) (lambda (a) (set! y a)))))) (car (lambda (f) (f (lambda (x y sx sy) (x))))) (cdr (lambda (f) (f (lambda (x y sx sy) (y))))) (set-car! (lambda (f a) (f (lambda (x y sx sy) ((sx a) f))))) (set-cdr! (lambda (f a) (f (lambda (x y sx sy) ((sy a) f)))))) ( cdr (set-cdr! (cons 1 2) 3)))"));
//    }
}
