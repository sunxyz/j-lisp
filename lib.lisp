(
    (`(define-macro when (lambda
        (test . branch)
        (apply ( list (quote if) test branch))))
    (define-macro unless  (lambda
        (test . branch)
        (apply ( list (quote if) (cons (quote not) test) branch))))
    )
    (define println (lambda (x) (
        (display x)
        (newline)
    )))
    (define map (lambda (f l)(list-map l f)))
    (define-macro defun (lambda (name args . body) (
        `(
            define ,name (lambda ,args ,body)
        )
    )))
    (define func defun)
    (define for (lambda-lep (fl . prc) (
       (define *def* (list-ref fl 0))
       (define *predicate* (list-ref fl 1))
       (define *set*  (list-ref fl 2))
       (apply (`(let
              (( ,(car *def*) ,(car (cdr *def*))))
              (while (,@*predicate*) (
                  ,prc
                  (set! ,(car *set*) ,(car (cdr *set*)))
              ))
       )))
   )))
   (define-macro loop (lambda (. exp) (
        `(while (#t) ,exp)
   )))
   (define-macro export(lambda (. exports) (
       ` (dict (list ,@(map symbol->string exports)) (list ,@exports))
   )))
   (define-macro import(lambda (names form file)(
        (define export-info (load file))
        (`(,@(map (lambda (n) (`(define ,n ,(dict-get export-info (symbol->string n))))) names)))
   )))
   ;(import (fun-a a b) form 'export-test.lisp')
   ;(display (fun-a a b))
)