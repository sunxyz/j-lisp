(
    ; 寄存器
    (func mark-empty-list () (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
    ))

    (define _$pc (mark-empty-list))
    (func $pc()(_$pc))
    (func $pc-write (word) (
        (list-foreach word (lambda (v index) (
            (list-set! _$pc index v)
        )))
    ))

    (define _$ (list))
    (for ((i 0)(< i 32) (i (+ i 1))) (
        list-add _$ (mark-empty-list)
    ))

    (func $write (bitIndex word) (
        (define index (_two2ten byteIndex))
        (list-set! _$ index word)
    ))

    (func $ (bitIndex) (
        (define index (_two2ten bitIndex))
        (list-ref _$ index)
    ))

    (define _word_size 32)
    (define _byte_size 8)
    (define _memory (mark-boolean-list (* 8 1024)))

    (func memory-write (byteIndex word) (
        (define index  (* (_two2ten byteIndex) _byte_size))
        (list-foreach word (lambda (o i) (
            list-set! _memory (+ index i) o
        )))
    ))

    (func memory-read (byteIndex) (
        (define left (* (_two2ten byteIndex) _byte_size))
        (list-sub _memory left (+ left _word_size))
    ))

    (func _two2ten (bit-ls) (
        (define len (length bit-ls))
        (define n 0)
        (for ((i 0) (< i len) (i (+ i 1))) (
            (set! n (+ (boolean->number (list-ref bit-ls i)) (* n 2)))
        ))
        n
    ))

    (func completion-word(ls)(
        (define tmp (mark-empty-list))
        (define len (length ls))
        (define left (- _word_size len))
        (for ((i 0) (< i len) (i (+ i 1))) (
            (list-set! tmp (+ left i) (list-ref ls i))
        ))
        tmp
    ))
    (export $pc-write $pc  $write $ memory-write memory-read mark-empty-list completion-word)
)