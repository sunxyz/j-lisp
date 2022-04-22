(
    (load 'mips/booleans.lisp')
    (load 'mips/alu.lisp')
    (import ($pc-write $pc  $write $ memory-write memory-read mark-empty-list completion-word) from 'mips/base.lisp')
    (import (r-instruct) from 'mips/r-format.lisp')
    (import (i-instruct) from 'mips/i-format.lisp')
    (import (j-instruct) from 'mips/j-format.lisp')
    (define _4 (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0
    ))
    (define _1 (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1
    ))
    (define _2 (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0
    ))
    (define $zero ($ (list 0 0 0 0 0)))
    (define $ra ($ (list 1 1 1 1 1)))
    (func move (la lb) (
        (add la $zero lb)
    ))
    (func pcAdd4()(
        (define temp (mark-empty-list))
        (add ($pc) _4 temp)
        ($pc-write temp)
    ))

    (func id(word)(
        (define op (list-sub word 0 6))
        (define r-i (list 0 0 0 0 0 0))
        (define j-i-0 (list 0 0 0 0 1 0))
        (define j-i-1 (list 0 0 0 0 1 1))
        ;执行
        (if (eqv? op r-i)
            (r-instruct word)
            (if (or (eqv? op j-i-0) (eqv? op j-i-1))
                (j-instruct word)
                (i-instruct word)))
    ))

    (func start(data)(
        (memory-write $zero data)
        (define next #t)
        (define word nil)
        (while (next) (
            (set! word (memory-read ($pc)))
            (display 'pc:')
            (display ($pc))
            (display 'word:')
            (display word)
            (newline)
            (when (null? (car word)) (
                (set! next #f)
            ))
            (when (next) (
                 (id word)
                 (pcAdd4)
            ))
        ))
    ))

    (start (load 'mips/set.data'))
)