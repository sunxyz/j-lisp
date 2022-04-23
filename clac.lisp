(
    (func println (x) (
        (display x)
        (newline)
    ))
    (func ten2two (t) (
        (define l (mark-list 8))
        (copy r-zero l)
        (define i (length l))
        (while (< 0 t)(
            (set! i (- i 1))
            (list-set! l i (% t 2))
            (set! t (/ t 2))
        ))
        l
    ))
    (func getEmptyList () (
        list 0 0 0 0 0 0 0 0
    ))
    ; 寄存器 不去实现 用 list 代替 r0-r8 move a b
    (define zero 0)
    (define r-a (mark-list 8))
    (define r-b (mark-list 8))
    (define r-c (mark-list 8))
    (define r-d (mark-list 8))
    (define r-e (mark-list 8))
    (define r-temp (mark-list 8))
    (define r-opt (getEmptyList))
    (define r-zero (list 0 0 0 0 0 0 0 0))
    (func copy (l r) (
        (define len (length l))
        (define r-len (length r))
        (for ((i 0) (< i len) (i (+ i 1))) (
            (list-set! r (- r-len i 1) (list-ref l (- len i 1)))
        ))
    ))
    ;左移
    (func sl(l num) (
        (define len (length  l))
        (for((i num)(< i len)(i (+ i 1)))(
            (list-set! l (- i num) (list-ref l i))
        ))
        ; 后几位 是 0
        (for ((i (- len num)) (< i len) (i (+ i 1))) (
            list-set! l i 0
        ))
    ))
     ;只用 and or not 作为基础
    ;(func and (a b) (and a b))
    ;(func or (a b) (or a b))
    ;(func not (a) (not a))
    (func xor (a b) (not (or a b)))
    (func xand (a b) (not (and a b)))
     ;--先实现计算单元
    (define add ((lambda () (
        (func not-eq (a b ) (
            and
            (xand a b)
            (or a b)
        ))
       (func not-eq-and  (a b) (
           (define j (and a b))
           (define p (not-eq a b))
           (cons j p)
        ))
        (func all-add  (a b j) (
            (define r1 (not-eq-and a b))
            (define r2 (not-eq-and (cdr r1) j))
            (cons  (or (car r1) (car r2)) (cdr r2))
         ))
         (lambda (l-a l-b l-r) (
            (define j 0)
            (define cons0 nil)
            (copy r-zero l-r)
            (
                for ((i (- (length l-a) 1)) (< -1 i) (i (- i 1))) (
                    (set! cons0 (all-add (list-ref l-a i) (list-ref l-b i) j))
                    (set! j (car cons0))
                    (list-set! l-r i (cdr cons0))
                 )
            )
         ))
     ))))
     (func multi (l-a l-b l-r) (
        ; todo 支持负数运算
         (define j nil)
         (define len (length l-b))
         (define l-0 (mark-list 8))
         (define l-1 (mark-list 8))
         (copy r-zero l-r)
         (for ((i (- len 1)) (< -1 i) (i (- i 1))) (
            (set! j (- len i 1))
            (if (less? zero (list-ref l-b i)) (
                   (copy l-a l-0)
                   (sl l-0 j)
                   (add l-0 l-r l-1)
                   (copy l-1 l-r)
            ))
         ))
         l-r
     ))
     ;负数单元
     (define toNegative(lambda (list l-r) (
           (add (map (lambda (o) (not o)) list) (map (lambda (o i) (eqv? i (- (length list) 1))) list) l-r)
     )))
     ;--实现控制单元

     (func eq? (a b) (
        or (and a b) (xor a b)
     ))
     (func eql? (l-a l-b) (
          (define r 1)
          (for((i 0)(< i (length l-a))(i (+ i 1))) (
            (set! r (and r (eq? (list-ref l-a i) (list-ref l-b i))))
          ))
          r
     ))
     (func less? (a b) (
        and (xor a zero) (or b zero)
     ))
     (define-macro if (lambda(p exp) (
        `(and ,p ,exp)
     )))
     ;5位长就够了 0开头的是数字 1 开头的是指令 + - * /
    (func inputNumber (l-a l-r) (
        ; r = (r*10)+a
        (define ten (list 0 0 0 0 1 0 1 0))
        (multi l-r ten r-e)
        (add r-e l-a r-temp)
        (copy r-temp l-r)
    ))
    (func clac (l-a l-r) (
        ; 0 + 1 - 2 * 3 /
        (if (eql? l-r (list 0 0 0 1 0 0 0 0)) (
            (add r-a r-b r-temp)
            (copy r-temp r-a)
        ))
        (if (eql? l-r (list 0 0 0 1 0 0 0 1)) (
             (toNegative r-a r-e)
             (copy r-e r-a)
             (add r-a r-b r-temp)
             (copy r-temp r-a)
        ))
        (if (eql? l-r (list 0 0 0  1 0 0 1 0)) (
            (multi r-a r-b r-temp)
            (copy r-temp r-a)
        ))
        (copy l-a l-r)
        (copy r-a r-b)
        (copy r-zero r-a)
    ))
    (func input (l) (
        (define car0 (car l))
        (if (eq? zero car0) (
            (define l0 (getEmptyList))
            (copy l l0)
            (inputNumber l0 r-a)
        ))
        (if (less? zero car0) (
            (clac l r-opt)
        ))
    ))
    (func show () (
        (if (eql? r-a r-zero) (
            println r-b
        ))
        (if (not (eql? r-a r-zero)) (
            println r-a
        ))
    ))

     ;测试 add
    (copy (list 0 0 0 0 1 0 1 0 ) r-a)
    (copy (list #f #f #f #f #f #f #f 1 ) r-b)
    ;(println r-a)
    (add r-a r-b r-c)
   ; ( r-a r-b r-c)
    ;(println r-c)
    ; 测试左移
    (define a0 (list 0 0 0 0 0 0 0 1 ) )
    (sl a0 3)
   ; (println a0)
    ;(println (ten2two 10))
    ;测试 multi
    (copy r-zero r-c)
    ;(multi  r-b r-a r-c)
    ;(println r-c)
     (copy r-zero r-a)
    (input (list 0 0 0 0 1))
    (input (list 0 0 0 0 1))
    (input (list 1 0 0 0 1))
    (input (list 0 0 0 1 1))
    (input (list 1 0 0 0 0))
    (show)
    (define 定义 define)
    (定义 函数 lambda)
    (定义 打印 display)
    (定义 李总 '好人')
    (定义 你好 (函数 (x) (
        打印(string-append x ': 你好')
    )))
    (你好 李总)
)