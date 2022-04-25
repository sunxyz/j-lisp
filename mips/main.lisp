(
    (load 'mips/booleans.lisp')
    (load 'mips/alu.lisp')
    (import (assembler) from 'mips/assembler.lisp')
    (import ($pc-store $pc  $store $ memory-store memory-load make-word completion-word) from 'mips/base.lisp')
    (import (r-instruct) from 'mips/r-format.lisp')
    (import (i-instruct) from 'mips/i-format.lisp')
    (import (j-instruct) from 'mips/j-format.lisp')

    (define _1 (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1
    ))
    (define _2 (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0
    ))
    (define _4 (
        list 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0
    ))

    (define $zero ($ (list 0 0 0 0 0)))
    (define $ra ($ (list 1 1 1 1 1)))

    (func move (la lb) (
        (add la $zero lb)
    ))
    (func pcAdd4()(
        (define temp (make-word))
        (add ($pc) _4 temp)
        ($pc-store temp)
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
        (memory-store $zero data)
        (define next #t)
        (define word nil)
        (while (next) (
            (set! word (memory-load ($pc)))
            (when (null? (car word)) (
                (set! next #f)
            ))
            (when (next) (
                (println 'pc:' ($pc) '  word:' word)
                (id word)
                (pcAdd4)
            ))
        ))
    ))

    (func load-file-lines-2-list (file_name ) (
        (define data (list))
        (call-with-input-file file_name (lambda (input_stream) (
            (for ((l (read-line input_stream)) (not (null? l)) (l (read-line input_stream)) ) (
                (list-add data l)
            ))
        )))
        (data)
    ))

    (func skip-note(line g) (
        (when (and (string-index-of line ';') (length line)) (
            g line
        ))
    ))

    (func file-lines-2-list (lines) (
        (define data (list))
        (list-foreach lines (lambda (line) (skip-note line (lambda (x) (
           (list-add-all data (string->list x))
        )))))
        data
    ))

    (start (file-lines-2-list (load-file-lines-2-list 'mips/set.bc')))
    ($pc-store $zero )
    (start (assembler (load-file-lines-2-list 'mips/set.ac')))
)