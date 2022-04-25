(

    (import (get-r-opt-code get-i-opt-code get-j-opt-code get-reg-code ten2two) from 'mips/instructions-dict.lisp')

    (func assembler (data) (
        (println 'begin assembler')
        (define binary_codes (list))
        (define binary_instr nil)
        (list-foreach data (lambda (line) (skip-note line (lambda (x) (

            (set! binary_instr (assembly (string-split line ' ')))
            (display (string-split line ' '))
            (display ':=>')
            (display binary_instr)
            (newline)
            (list-add-all binary_codes binary_instr)
        )))) )
        (println 'end assembler')
        binary_codes
    ))

    (func assembly(instr) (
        (define r_opt_code (get-r-opt-code (list-ref instr 0)))
        (define binary_codes nil)
        (if (notnull? r_opt_code) (
            (set! binary_codes (map
                                (lambda (x) (string->number x))
                                (string->list(string-append '000000' (get-reg-code (list-ref instr 2)) (get-reg-code (list-ref instr 3)) (get-reg-code (list-ref instr 1)) '00000' r_opt_code))))
        ) (
            (define i_opt_code (get-i-opt-code (list-ref instr 0)))
            (if (notnull? i_opt_code) (
                (set! binary_codes (map
                                    (lambda (x) (string->number x))
                                    (string->list(string-append i_opt_code (get-reg-code (list-ref instr 2)) (get-reg-code (list-ref instr 1)) (list->string(ten2two (string->number(list-ref instr 3)) 16))))))
            ) (
                (define j_opt_code (get-j-opt-code (list-ref instr 0)))
                (set! binary_codes (map
                                    (lambda (x) (string->number x))
                                    (string->list(string-append j_opt_code (list->string(ten2two (string->number(list-ref instr 1)) 26))))))
            ))
        ))
        binary_codes
    ))

    (func skip-note(line g) (
        (when (and (string-index-of line ';') (length line)) (
            g line
        ))
    ))

    (func notnull? (x) (not (null? x)))

    (export assembler)
)