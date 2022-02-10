(
    (define cons (lambda (x y) (lambda(g)(g x y))))
    (define car (lambda (f) (f (lambda(x y)(x)))))
    (define cdr (lambda (f) (f (lambda(x y)(y)))))
    (define if (lambda (p then_v else_v) ((or (and p car) cdr) (cons then_v else_v))))
)