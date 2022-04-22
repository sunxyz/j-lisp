(

    (func i-addi (rs rt immediate) (
        (add rs immediate rt)
    ))

    (func i-andi (rs rt immediate) (
        covert(and rs immediate rt )
    ))

    (func i-ori (rs rt immediate) (
        covert(or rs immediate rt )
    ))

    (func i-xori (rs rt immediate) (
        covert(xor rs immediate rt)
    ))

    (func i-lw (rs rt immediate) (
        move (memory-read (add rs immediate)) rt
    ))

    (func i-sw (rs rt immediate) (
        move  rt (memory-read (add rs immediate))
    ))

    (func i-beq (rs rt immediate) (
        (when (eqv? rs rt) (
            $pc-write (sll immediate _2)
        ))
    ))

    (func i-bne (rs rt immediate) (
        (unless (eqv? rs rt) (
            $pc-write (sll immediate _2)
        ))
    ))

    (func i-slti (rs rt immediate) (
        (define tmp (make-word ))
        (sub immediate rs  tmp)
        ; 第一位是0代表是非负数 如果是非负数 说明 rt>rs
        (when (eq? car tmp #f) (
            (move _1 rt)
        ))
        (unless (eq? car tmp #f) (
           (move $zero rt)
        ))
    ))


    (func covert (f rs rt rd) (
        (list-foreach rs (lambda (a index ) (
            (define b (list-ref rt index))
            (list-set! rd index (f a b))
        )))
    ))

   (define mapper (make-dict))
   (dict-put! mapper (list->string(list 0 0 1 0 0 0)) i-addi)
   (dict-put! mapper (list->string(list 0 0 1 1 0 0)) i-andi)
   (dict-put! mapper (list->string(list 0 0 1 1 0 1)) i-ori)
   (dict-put! mapper (list->string(list 0 0 1 1 1 0)) i-xori)
   (dict-put! mapper (list->string(list 1 0 0 0 1 1)) i-lw)
   (dict-put! mapper (list->string(list 1 0 1 0 1 1)) i-sw)
   (dict-put! mapper (list->string(list 0 0 0 1 0 0)) i-beq)
   (dict-put! mapper (list->string(list 0 0 0 1 0 1)) i-bne)
   (dict-put! mapper (list->string(list 0 0 1 0 1 0)) i-slti)

   (func i-instruct (ls) (
         (define op (list->string(list-sub ls 0 6)))
         (define rs ($ (list-sub ls 6 11)))
         (define rt ($ (list-sub ls 11 16)))
         (define immediate (completion-word(list-tail ls 16)))
         (define opf (dict-get mapper op))
         (opf rs rt immediate)
   ))

   (export i-instruct)
)