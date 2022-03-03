package org.yangrd.lab.lisp.type;

import lombok.EqualsAndHashCode;
import lombok.Value;

public interface Strings{

    String getVal();

    Character[] toCharArray();

    Strings replaceAllSpace();

    static Strings of(String str) {
        return new SimpleStr(str);
    }

    @Value
    @EqualsAndHashCode
    class SimpleStr implements Strings {
        String val;

        @Override
        public Character[] toCharArray() {
            char[] chars = val.toCharArray();
            Character[] characters = new Character[chars.length];
            for (int i = 0; i < characters.length; i++) {
                characters[i] = chars[i];
            }
            return characters;
        }

        @Override
        public Strings replaceAllSpace() {
            return Strings.of(val.replaceAll(" ",""));
        }

        @Override
        public String toString() {
            return "'" + val+"'";
        }


    }
}
