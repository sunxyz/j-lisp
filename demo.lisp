(
    // 翻译
       (define methods (make-dict))
       (define struct-head (cons (cons `struct `struct-name) (cons `field-names-vector methods)))
       (define-macro struct (lambda (. fields)
         //(set-cdr! (car struct-head) (hack-struct-name fields))
         (set-car! (cdr struct-head) (list->vector fields))
         (map (lambda (f index)
                (dict-put! methods (string->symbol (string-append 'get-' (symbol->string f))) (cons `self (lambda (o) (vector-ref (cdr o) index))))
                (dict-put! methods (string->symbol (string-append 'set-' (symbol->string f))) (cons `self (lambda (o v) (vector-set! (cdr  o) index v))))
               )
              fields)
         (lambda (. field-values)
            (define val  (cons struct-head (list->vector field-values)))
            (define self (lambda (key . msg) (
                (define method-info (dict-get methods key))
                (if (pair? method-info)
                    (
                        (define method (cdr method-info))
                        (define args nil)
                        (if  (eqv? `self (car method-info))
                             (
                                 (set! args (list val))
                                 (list-add-all args msg)
                             )
                             (
                                (set! args (list self))
                                (list-add-all args msg)
                             )
                        )
                        (apply method args)
                    )
                    (error (string-append 'not method  ' (symbol->string key)))
                )
            )))
            self)
       ))
       (define-macro func (lambda (. data) (
            if (exp? (car data))
            (
                (define v (list->vector data))
                (define struct-name (car (vector-ref v 0)))
                (define func-name (vector-ref v 1))
                (define func-params (vector-ref v 2))
                (define func-body (cdr (cdr (cdr data))))

                (define temp-func-params (list struct-name))
                (map (lambda (param) (list-add temp-func-params param))  func-params)
                (set! func-params temp-func-params)
                (dict-put! methods func-name
                 (cons `func (apply (`(lambda ,func-params ,@func-body))))
                )
            )
           (`(defun ,@data)))
       )))
       (define-macro new (lambda (f . vs)
         ((apply f) vs)
       ))
      ;定义结构体
       (define dog (struct (name age color)))
      ; 定义方法
       (func (dog) hello (a b c) (
             (println (string-append 'hello: ' (dog `get-name) a b c))
       ))
       (func hello (a b c) (
                (println (string-append 'hello:' a b c))
       ))
       (hello  'a' 'b' 'c')
       (define dog-obj (dog '狗子' 5 '白色'))
       (define dog-obj0 (new dog('狗子55' 5 '白色')))
       (println (dog-obj0 `get-name))
       (dog-obj `hello (`( 'a' 'b' 'c')))
       (dog-obj `hello  'a' 'b' 'c')
       (println (dog-obj `get-name))
       (dog-obj `set-name '狗子0')
       (println (dog-obj `get-name))
       (println (dog-obj `get-name))
       (dog-obj `set-name ('狗子0'))


         (define tcp-listen (socket '192.168.8.0:8080'))
           (tcp-listen  (lambda (req) (
               (req)
               (res)
           ))
           (define server-listen (http-server tcp-listen))
           (server-listen (lambda (req) (
               (write-html-body  'hello world'  (get-location reg))
           )))
           (define http-request struct (
               (head dict)
               (body string)
           ))
           (func (this http-request) get-location () (
               (dict-get (get-dict this) 'location')
           ))

           (func (this http-request) get-body0 () (
                (this get-body args ) => (get-body this args)
           ))
           (func @ (s f . args) (
               apply f s args
           ))
            (this.xxx y b => (xxx this y b))
            (this.xxx(y b) => (apply xxx this (y b)))
)