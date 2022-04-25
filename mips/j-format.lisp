(

    (func j-j (address) (
        (define tmp (make-word))
        (move ($pc) tmp)
        (sll address _2)
        (for ((i 4) (< i 32) (i (+ i 1))) (
            (list-set! tmp i (list-ref address (- i 4)))
         ))
        ($pc-store tmp)
    ))

    (func j-jal (address) (
        (move ($pc) $ra)
        (define tmp (make-word))
        (move ($pc) tmp)
        (sll address _2)
        (for ((i 4) (< i 32) (i (+ i 1))) (
            (list-set! tmp i (list-ref address (- i 4)))
        ))
        ($pc-store $pc tmp)
    ))

    (define mapper (make-dict))
    (dict-put! mapper (list->string(list 0 0 0 0 1 0)) j-j)
    (dict-put! mapper (list->string(list 0 0 0 0 1 1)) j-jal)

    (func j-instruct (ls) (
        (define op (list->string(list-sub 0 6)))
        (define address (list-tail ls 6))
        ((dict-get mapper op) address)
    ))

    (export j-instruct)
)