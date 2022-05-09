(
    (define _*array* (list 0 0 0 0 0 0 0 0))
    (define _*array_point* 0)

    (func read-val () (
        list-ref _*array* _*array_point*
    ))
    (func write-val (v) (
        list-set! _*array* _*array_point* v
    ))

    (func point-> () (
        set! _*array_point* (+ _*array_point* 1)
    ))
    (func point<- () (
        set! _*array_point* (- _*array_point* 1)
    ))
    (func increment () (
        (write-val (+ (read-val) 1))
    ))
    (func decrease () (
        (write-val (- (read-val) 1))
    ))
    (func write-console () (
        (display (number->char(read-val)))
    ))
    ; todo
    (func read-console () (
        (write-val (read-line))
    ))

    (func while? () (
        (> (read-val) 0)
    ))

    (func interpreter(*ast-codes*)(
        (define *code* (ast-get-data *ast-codes*))
        (list-foreach *code* (lambda (*c*) (
            (cond
                ((eqv? *c* '>') (point->))
                ((eqv? *c* '<') (point<-))
                ((eqv? *c* '+') (increment))
                ((eqv? *c* '-') (decrease))
                ((eqv? *c* '.') (write-console))
                ((eqv? *c* ',') (read-console))
                ((ast? *c*) (
                    (while (while?)
                    (interpreter *c*))
                ))
                (else (display *c*))
            )
        )))
    ))

    (export interpreter)
)