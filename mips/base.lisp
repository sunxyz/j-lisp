(
    ; 寄存器
    (define _register_qty 32)
    (func make-word () (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
    ))

    (define _$pc (make-word ))
    (func $pc()(_$pc))
    (func $pc-store (word) (
        (list-foreach word (lambda (v index) (
            (list-set! _$pc index v)
        )))
    ))


    (define _$ (make-list _register_qty))
    (for ((i 0)(< i _register_qty) (i (+ i 1))) (
        list-set! _$ i (make-word )
    ))

    (func $store (bitIndex word) (
        (define index (two2ten byteIndex))
        (list-set! _$ index word)
    ))

    (func $ (bitIndex) (
        (define index (two2ten bitIndex))
        (list-ref _$ index)
    ))

    ;内存
    (define _word_size 32)
    (define _byte_size 8)
    (define _memory (make-boolean-list (* 8 1024)))

    (func memory-store (byteIndex word) (
        (define index  (* (two2ten byteIndex) _byte_size))
        (list-foreach word (lambda (o i) (
            list-set! _memory (+ index i) o
        )))
    ))

    (func memory-load (byteIndex) (
        (define left (* (two2ten byteIndex) _byte_size))
        (list-sub _memory left (+ left _word_size))
    ))

    ;补全
    (func completion-word(ls)(
        (define tmp (make-word ))
        (define len (length ls))
        (define left (- _word_size len))
        (for ((i 0) (< i len) (i (+ i 1))) (
            (list-set! tmp (+ left i) (list-ref ls i))
        ))
        tmp
    ))
    (export $pc-store $pc  $store $ memory-store memory-load make-word  completion-word)
)