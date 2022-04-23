(
    (func ten2two (t bit-size) (
        (define l (make-list bit-size))
        (list-foreach l (lambda (o i) (list-set! l i 0)))
        (define i (length l))
        (while (< 0 t)(
            (set! i (- i 1))
            (list-set! l i (% t 2))
            (set! t (/ t 2))
        ))
        l
    ))
    ;opt
    (define _r_opt_dict (make-dict))
    (dict-put! _r_opt_dict 'add' '100000')
    (dict-put! _r_opt_dict 'sub' '100010')
    (dict-put! _r_opt_dict 'and' '100100')
    (dict-put! _r_opt_dict 'or' '100101')
    (dict-put! _r_opt_dict 'xor' '100110')
    (dict-put! _r_opt_dict 'nor' '100111')
    (dict-put! _r_opt_dict 'slt' '101010')
    (dict-put! _r_opt_dict 'sll' '000000')
    (dict-put! _r_opt_dict 'srl' '000010')
    (dict-put! _r_opt_dict 'sllv' '000100')
    (dict-put! _r_opt_dict 'srlv' '000110')
    (dict-put! _r_opt_dict 'jr' '001000')
    (dict-put! _r_opt_dict 'syscall' '001100')

    (define _i_opt_dict (make-dict))
    (dict-put! _i_opt_dict 'addi' '001000')
    (dict-put! _i_opt_dict 'andi' '001100')
    (dict-put! _i_opt_dict 'ori' '001101')
    (dict-put! _i_opt_dict 'xori' '001110')
    (dict-put! _i_opt_dict 'lw' '100011')
    (dict-put! _i_opt_dict 'sw' '101011')
    (dict-put! _i_opt_dict 'beq' '000100')
    (dict-put! _i_opt_dict 'bne' '000101')
    (dict-put! _i_opt_dict 'slti' '001010')

    (define _j_opt_dict (make-dict))
    (dict-put! _j_opt_dict 'j' '000010')
    (dict-put! _j_opt_dict 'jal' '000011')

    (func get-r-opt-code (opt_name) (
        dict-get _r_opt_dict opt_name
    ))
    (func get-i-opt-code (opt_name) (
        dict-get _i_opt_dict opt_name
    ))
    (func get-j-opt-code (opt_name) (
        dict-get _j_opt_dict opt_name
    ))
    ;reg
    (define _reg_list (list '$zero' '$at' '$v0' '$v1' '$a0' '$a1' '$a2' '$a3' '$t0' '$t1' '$t2' '$t3' '$t4' '$t5' '$t6' '$t7' '$s0' '$s1' '$s2' '$s3' '$s4' '$s5' '$s6' '$s7' '$t8' '$t9' '$k0' '$k1' '$gp' '$sp' '$fp' '$ra'))
    (define _reg_dict (make-dict))
    (map (lambda (key index) (
        (dict-put! _reg_dict key (list->string(ten2two index 5)))
    )) _reg_list)
    (func get-reg-code (reg_name) (
        (dict-get _reg_dict reg_name)
    ))

    (export get-r-opt-code get-i-opt-code get-j-opt-code get-reg-code ten2two)
)