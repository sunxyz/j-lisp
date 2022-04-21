(

    (func add (la lb lc)(

        (func half-plus-bit (bit-a bit-b)(
            ; 进位 当前位
            (cons (and bit-a bit-b) (xor bit-a bit-b))
        ))
        (func all-plus-bit (bit-a bit-b bit-j)(
            (define r1 (half-plus-bit bit-a bit-b))
            (define r2 (half-plus-bit (cdr r1) bit-j))
            (cons  (or (car r1) (car r2)) (cdr r2))
        ))

        (define bit-j #f)
        (define cons-info nil)
        (define len (-(length la) 1))
        (define index nil)
        (list-foreach la (lambda (o i)(
            (set! index (- len i))
            (set! cons-info (all-plus-bit (list-ref la index) (list-ref lb index) bit-j))
            (set! bit-j (car cons-info))
            (list-set! lc index (cdr cons-info))
        )))
        lc
    ))

    (func sub (la lb lc)(
        (define rd mark-empty-list)
        (add la  (to-negative lb rd) lc)
    ))

     ;负数单元
      (func to-negative(rs rd) (
            // 取反 + 1
            (add (map not rs) _1 rd)
            rd
      ))

      ;左移
      (func sll(l bitNum) (
          (define num (_two2ten  bitNum))
          (define len (length  l))
          (for((i num)(< i len)(i (+ i 1)))(
              (list-set! l (- i num) (list-ref l i))
          ))
          ; 后几位 是 0
          (for ((i (- len num)) (< i len) (i (+ i 1))) (
              list-set! l i 0
          ))
      ))
      ; 右移
      (func srl(l bitNum) (
          (define num (_two2ten  bitNum))
          (define len (length  l))
          (for((i (- len num))(< num i)(i (- i 1)))(
              (list-set! l (+ i num) (list-ref l i))
          ))
          ; 前几位 是 0
          (for ((i 0) (< i num) (i (+ i 1))) (
              list-set! l i 0
          ))
      ))

     (func _two2ten (ls) (
          (define len (length ls))
          (define n 0)
          (for ((i 0) (< i len) (i (+ i 1))) (
              (set! n (+ (boolean->number (list-ref ls i)) (* n 2)))
          ))
          n
     ))
)