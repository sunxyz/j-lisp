package org.yangrd.lab.lisp;

import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.atom.Booleans;
import org.yangrd.lab.lisp.atom.Strings;
import org.yangrd.lab.lisp.atom.Symbols;
import org.yangrd.lab.lisp.support.ConsMarker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Parse {

    private static final String PREFIX = "(";
    private static final String SUFFIX = ")";
    private static final List<String> FLAT_SYMBOLS = Arrays.asList(",@", ",");
    private static final List<String> SPLIT_SYMBOLS = Collections.singletonList("`");

    public static Cons parse(String expStr) {
        return parse(format(replaceSpace(expStr.replaceAll("\"", "'"))), Cons.newInstance(null));
    }

    private static Cons parse(String expStr, Cons r) {
        String tempExpStr = expStr.trim();
        boolean isPush = tempExpStr.indexOf(PREFIX) == 0;
        String nextStr = tempExpStr.substring(1);
        int prefixIndex2 = nextStr.indexOf(PREFIX);
        int suffixIndex2 = nextStr.indexOf(SUFFIX);
        int nextFixIndex = (prefixIndex2 == -1 || suffixIndex2 == -1 ? Math.max(prefixIndex2, suffixIndex2) : Math.min(prefixIndex2, suffixIndex2));
        if (nextFixIndex > -1) {
            String subExpStr = nextStr.substring(0, nextFixIndex);
            if (isPush) {
                Cons astList = Cons.newInstance(r);
                exp2List(subExpStr, astList).forEach(astList::add);
                r.add(astList);
                return parse(tempExpStr.substring(nextFixIndex + 1), astList);
            } else {
                exp2List(subExpStr, r.parent()).forEach(r.parent()::add);
                return parse(tempExpStr.substring(nextFixIndex + 1), r.parent());
            }
        } else {
            return r;
        }
    }

    private static List<Object> exp2List(String exp, Cons parent) {
        return exp.trim().length() == 0 ? Collections.emptyList() : Arrays.stream(exp.trim().split(" ")).map(Parse::parseObj).map(o -> o instanceof Cons ? ConsMarker.markExp(parent, ((Cons) o).data().toArray()) : o).flatMap(o -> o instanceof List ? ((List<?>) o).stream() : Stream.of(o)).collect(Collectors.toList());
    }

    private static String format(String str) {
        return str.contains("  ") ? format(str.replaceAll("  ", " ")) : str;
    }

    private static String replaceSpace(String str) {
        if (str.contains("'")) {
            int first = str.indexOf("'");
            int last = str.substring(first + 1).indexOf("'") + first + 1;
            String substring = str.substring(first, last + 1);
            substring = substring.replaceAll(" ", "\\\\\\O");
            return str.substring(0, first) + substring + replaceSpace(str.substring(last + 1));
        }
        return str;
    }

    private static Object parseObj(String atom) {
        if (Pattern.compile("-?\\d+").matcher(atom).matches()) {
            return Integer.valueOf(atom);
        } else if (atom.equals("#t") || atom.equals("#f")) {
            return Booleans.of(atom.equals("#t"));
        } else if (atom.indexOf("'") == 0 && atom.lastIndexOf("'") == atom.length() - 1) {
            return Strings.of(atom.replaceAll("'", "").replaceAll("\\\\O", " "));
        } else {
            Optional<String> first = SPLIT_SYMBOLS.stream().filter(o -> atom.indexOf(o) == 0).findFirst();
            Optional<String> flatFirst = FLAT_SYMBOLS.stream().filter(o -> atom.indexOf(o) == 0).findFirst();
            return first.isPresent() && first.get().length() < atom.length() ? ConsMarker.markList(Symbols.of(first.get()), parseObj(atom.substring(first.get().length()))) : (flatFirst.isPresent() && flatFirst.get().length() < atom.length() ? Arrays.asList(Symbols.of(flatFirst.get()), parseObj(atom.substring(flatFirst.get().length()))) : Symbols.of(atom));
        }
    }
}