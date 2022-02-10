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
        System.out.println(eval("((load 'lib.lisp' 'alias.lisp')(define fact (lambda (n)" +
                "  (if (= n 1)" +
                "      1" +
                "      (`(* n ((fact (- n 1)))))))) (fact 10))"));
        System.out.println(eval("((load 'lib.lisp' 'alias.lisp')((`(11))))"));

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
