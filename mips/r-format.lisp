(

    (func r-add (rs rt rd) (
        add rs rt rd
    ))

    (func r-sub (rs rt rd) (
        sub rs rt rd
    ))

    (func r-and (rs rt rd) (
        covert(and rs rt rd )
    ))

    (func r-or (rs rt rd) (
        covert(or rs rt rd )
    ))

    (func r-xor (rs rt rd) (
        covert(xor rs rt rd )
    ))

    (func r-nor (rs rt rd) (
        covert(nor rs rt rd)
    ))

    (func r-slt (rs rt rd) (
        (sub rt rs  rd)
        ; 第一位是0代表是非负数 如果是非负数 说明 rt>rs
        (when (eq? car rd #f) (
            (move _1 rd)
        ))
        (unless (eq? car rd #f) (
           (move $zero rd)
        ))
    ))

    (func r-sll (rs rt rd shamt) (
        (move rt rd)
        (sll rd shamt)
    ))

    (func r-srl (rs rt rd shamt) (
        (move rt rd)
        (srl rd shamt)
    ))

    (func r-sllv (rs rt rd ) (
        (move rs rd)
        (sll rd rt)
    ))

    (func r-srlv (rs rt rd ) (
        (move rs rd)
        (srl rd rt)
    ))

    (func r-jr (rs) (
        ($pc-write rs)
    ))

    (func r-syscall (rs rt rd shamt) (
        ;todo
       (display ':=>')
       (display rd)
       (newline)
    ))


    (func covert (f rs rt rd) (
        (list-foreach rs (lambda (a index ) (
            (define b (list-ref rt index))
            (list-set! rd index (f a b))
        )))
    ))

    (define mapper (mark-dict))
    (dict-put! mapper (list->string(list 1 0 0 0 0 0)) r-add)
    (dict-put! mapper (list->string(list 1 0 0 0 1 0)) r-sub)
    (dict-put! mapper (list->string(list 1 0 0 1 0 0)) r-and)
    (dict-put! mapper (list->string(list 1 0 0 1 0 1)) r-or)
    (dict-put! mapper (list->string(list 1 0 0 1 1 0)) r-xor)
    (dict-put! mapper (list->string(list 1 0 0 1 1 1)) r-nor)
    (dict-put! mapper (list->string(list 1 0 1 0 1 0)) r-slt)
    (dict-put! mapper (list->string(list 0 0 0 0 0 0)) r-sll)
    (dict-put! mapper (list->string(list 0 0 0 0 1 0)) r-srl)
    (dict-put! mapper (list->string(list 0 0 0 1 0 0)) r-sllv)
    (dict-put! mapper (list->string(list 0 0 0 1 1 0)) r-srlv)
    (dict-put! mapper (list->string(list 0 0 1 0 0 0)) r-jr)
    (dict-put! mapper (list->string(list 0 0 1 1 0 0)) r-syscall)

    (func r-instruct (ls) (

       (define rs ($ (list-sub ls 6 11)))
       (define rt ($ (list-sub ls 11 16)))
       (define rd ($ (list-sub ls 16 21)))
       (define shamt (completion-word(list-sub ls 21 26)))
       (define func0 (list->string(list-tail ls 26)))
       ((dict-get mapper func0) rs rt rd shamt)
    ))

    (export r-instruct)
)