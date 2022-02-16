(
    (define-macro when (lambda
        (test . branch)
        (apply ( list (quote if) test branch))))
    (define-macro unless  (lambda
        (test . branch)
        (apply ( list (quote if) (cons (quote not) test) branch))))
    (define map (lambda (g x . y)
            (cons
                (g (car x) (car y))
                (if (null? (cdr x))
                    (quote ())
                    (map g (cdr x) (if (null? y) (quote ()) (cdr y)))))))
)