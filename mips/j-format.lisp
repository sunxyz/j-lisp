(

    (func j-j (address) (
        (define tmp (mark-empty-list))
        (move $pc tmp)
        (sll address _2)
        (for ((i 4) (< i 32) (i (+ i 1))) (
            (list-set! tmp i (list-ref address (- i 4)))
         ))
        (move tmp $pc)
    ))

    (func j-jal (address) (
        (move $pc $ra)
        (define tmp (mark-empty-list))
        (move $pc tmp)
        (sll address _2)
        (for ((i 4) (< i 32) (i (+ i 1))) (
            (list-set! tmp i (list-ref address (- i 4)))
        ))
        (move tmp $pc)
    ))

    (define mapper (mark-dict))
    (dict-put! mapper (list 0 0 0 0 1 0) j-j)
    (dict-put! mapper (list 0 0 0 0 1 1) j-jal)

    (func j-instruct (ls) (
        (define op (list-sub 0 6))
        (define address (list-tail ls 6))
        ((dict-get mapper op) address)
    ))

    (export j-instruct)
)