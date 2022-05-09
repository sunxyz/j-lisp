(
    (import (make-ast ast-push ast-get-data  ast-get-parent ast?) from 'brainfuck/ast.lisp')
    (import (parse) from 'brainfuck/parser.lisp')
    (import (interpreter) from 'brainfuck/interpreter.lisp')

    (func main () (
       (interpreter (parse '+++++[>+++++++++++++<-]>.'))
    ))
    (main)
)