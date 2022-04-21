(
    ;只用 and or not 作为基础
    ;(func and (a b) (and a b))
    ;(func or (a b) (or a b))
    ;(func not (a) (not a))
    (func nand (a b) (not (and a b)))
    (func nor (a b) (not (or a b)))
    (func xor (a b) (not (or (and a b) (and (not a) (not b)))))
    (func eq? (a b) (not (xor a b)))
    (func less? (a b) ( and (nor a #f) (and b #t)))

    (define-macro when (lambda( p exp) (
        (`(and ,p ,exp))
    )))

    (define-macro unless (lambda( p exp) (
        `(or ,p ,exp)
    )))

    (func eqv? (rs rt)(
        (define l (map (lambda (v index ) (
           eq? v (list-ref rt index)
        )) rs))

        ;zip
        ;(for ((i 0) (< i (/ (length l) 2)) (i (+ i 1))) (
        ;    (eq? (list-ref l i) )
        ;))
        ;暂时简写了
        (apply and l)
    ))
)