package org.yangrd.lab.lisp.type;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Objects;

public interface Strings{

    String getVal();

    Character[] toCharArray();

    Strings replaceAllSpace();

    Integer indexOf(Strings str);

    Strings upcase();

    Strings downcase();

    Strings trim();

    Strings substitute(Strings dest, Strings resource);

    Strings subseq(Integer i, Integer from);

    Strings remove(Strings strings);



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
        public Integer indexOf(Strings str) {
            return val.indexOf(str.getVal());
        }

        @Override
        public Strings upcase() {
            return Strings.of(val.toUpperCase());
        }

        @Override
        public Strings downcase() {
            return Strings.of(val.toLowerCase());
        }

        @Override
        public Strings trim() {
            return Strings.of(val.trim());
        }

        @Override
        public Strings substitute(Strings dest, Strings resource) {
            return Strings.of(val.replaceAll(resource.getVal(), dest.getVal()));
        }

        @Override
        public Strings subseq(Integer i, Integer from) {
            return Strings.of(val.substring(i, from));
        }

        @Override
        public Strings remove(Strings strings) {
            return Strings.of(val.replaceAll(strings.getVal(),""));
        }

        @Override
        public String toString() {
            return "'" + val+"'";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleStr simpleStr = (SimpleStr) o;
            return Objects.equals(val, simpleStr.val);
        }

        @Override
        public int hashCode() {
            return Objects.hash(val);
        }
    }
}
