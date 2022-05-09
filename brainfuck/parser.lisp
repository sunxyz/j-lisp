(
    (func parse (*str*) (
       (define *ast* (make-ast nil))
       (parse0 (string->list(string-replace-all-space *str*)) *ast* 0)
       (*ast*)
    ))

    (func parse0 (*data* *ast* *index*) (
        (define *v* nil)
        (define *c* nil)
        (define next? #t)
        (for ((i *index*) (and (< i (length *data*)) next?) (i (+ i 1))) (
            (set! *v* (list-ref *data* i))
            (
                cond
                ((eqv? *v* '[')(
                    (set! next? #f)
                    (set! *c* (make-ast *ast* ))
                    (ast-push *ast* *c*)
                    (parse0 *data* *c* (+ i 1))
                ))
                ((eqv? *v* ']')(
                    (set! next? #f)
                    (parse0 *data* (ast-get-parent *ast*) (+ i 1))
                ))
                (else (ast-push *ast* *v*))
            )
        ))
    ))

    (export parse)
)