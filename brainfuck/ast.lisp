(
    (func make-ast (parent) (vector 'ast' (list) parent))
    (func ast-push (ast v) (
        (list-add (vector-ref ast 1) v)
    ))
    (func ast-get-data (ast) (
        (vector-ref ast 1)
    ))
    (func ast-get-parent (ast) (
        (vector-ref ast 2)
    ))
    (func ast? ()(
        (eqv? (vector-ref ast 0) 'ast')
    ))
    (export make-ast ast-push ast-get-data ast-get-parent)
)