(
    (define-macro when (lambda
        (test . branch)
        (apply ( list (quote if) test branch))))
    (define-macro unless  (lambda
        (test . branch)
        (apply ( list (quote if) (cons (quote not) test) branch))))
    (define map0 (lambda (g x . y)
            (cons
                (g (car x) (car y))
                (if (null? (cdr x))
                    (quote ())
                    (map g (cdr x) (if (null? y) (quote ()) (cdr y)))))))
    (define map (lambda (f l)(list-map l f)))

    (
        define-macro defstruct (lambda (x  .  ff) (
            (let (
                     (struct-name (symbol->string x))
                     (keys (map (lambda (x) (if (pair? x) (car x) (x))) ff))) (
                        `(
                             (define ,(string->symbol (string-append 'mark-' struct-name)) (lambda (,@keys) (
                                    (cons
                                     (vector 'struct' ,struct-name)
                                     (vector ,@keys)))))
                             (define ,(string->symbol (string-append 'verify-struct-' struct-name)) (lambda (o) (
                                (if
                                    (not (and (eqv? 'struct' (vector-ref (car o) 0)) (eqv? ,struct-name (vector-ref (car o) 1)) ))
                                    (error ,(string-append 'not-struct ' struct-name))))))
                             ,@(map (lambda (n i) (
                                (
                                    `(define ,(string->symbol (string-append struct-name '.' (symbol->string n))) (lambda (o) (
                                       (,(string->symbol (string-append 'verify-struct-' struct-name)) o)
                                       (vector-ref (cdr o) ,i)
                                    )))
                                )
                             )) keys)
                             ,@(map  (lambda (n i) (
                                 (
                                     `(define ,(string->symbol (string-append struct-name '.' (symbol->string n) '-set!')) (lambda (o v) (
                                        (,(string->symbol (string-append 'verify-struct-' struct-name)) o)
                                        (vector-set! (cdr o) ,i v)
                                     )))
                                 )
                              )) keys)))))))
)