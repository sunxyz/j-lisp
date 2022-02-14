(
    (define-macro when (lambda
        (test . branch)
        (apply ( list (quote if) test branch))))
    (define-macro unless  (lambda
        (test . branch)
        (apply ( list (quote if) (cons (quote not) test) branch))))
)