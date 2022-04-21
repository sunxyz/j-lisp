# mips 指令集实现

# r-type 

|  op   | rs  | rt  | rd  | shamt | func  |
|  ----  | ----  |-----| ----  |-------| ----  |
| 31..26  | 25..21 | 20..16 | 15..11 | 10..6 | 5..0 |
| 000000  | rs | rt  | rd | shamt | func |
- [x] add  func 100000
- [x] sub  func 100010
- [x] and  func 100100
- [x] or   func 100101
- [x] xor  func 100110
- [x] nor  func 100111
- [x] slt  func 101010
- [x] sll  func 000000
- [x] srl  func 000010
- [x] sllv  func 000100
- [x] srlv  func 000110
- [x] jr   func 001000
- [x] syscall func 001100

# i-type
|  op   | rs  | rt  | immediate |
|  ----  | ----  |-----|-----------|
| 31..26  | 25..21 | 20..16 | 15..0     |
| op  | rs | rt  | immediate        |

- [x] addi  op  001000
- [x] andi  op  001100
- [x] ori   op  001101
- [x] xori  op  001110
- [x] lw    op  100011
- [x] sw    op  101011
- [x] beq   op  000100
- [x] bne   op  000101
- [x] slti  op  001010

# j-type
|  op   | address |
|  ----  |---------|
| 31..26  | 25..0   |
| op  | address |

- [x] j     op  000010
- [x] jal   op  000011


