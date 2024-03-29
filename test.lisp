(
    (define cons (lambda (x y) (lambda(g)(g x y))))
    (define car (lambda (f) (f (lambda(x y)(x)))))
    (define cdr (lambda (f) (f (lambda(x y)(y)))))
    (define if (lambda (p then_v else_v) ((or (and p car) cdr) (cons then_v else_v))))
    (define lazy-fun (lambda (exp) (
    	lambda () (exp))))
    (define list (lambda (. data) (
        let ((o (car data)) (t (cdr data)))
        (cons o (if (null? t) (quote ()) (list t)))
    )))


)