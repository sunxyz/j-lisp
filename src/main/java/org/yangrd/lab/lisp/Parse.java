package org.yangrd.lab.lisp;

import lombok.extern.slf4j.Slf4j;
import org.yangrd.lab.lisp.atom.Strings;
import org.yangrd.lab.lisp.atom.Symbols;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Parse {

    private static final String PREFIX = "(";
    private static final String SUFFIX = ")";

    public static Cons parse(String expStr) {
        return parse(format(replaceSpace(expStr)), Cons.newInstance(null));
    }

    private static Cons parse(String expStr, Cons r) {
        String tempExpStr = expStr.trim();
        boolean isPush = tempExpStr.indexOf(PREFIX) == 0;
        String nextStr = tempExpStr.substring(1);
        int prefixIndex2 = nextStr.indexOf(PREFIX);
        int suffixIndex2 = nextStr.indexOf(SUFFIX);
        int nextFixIndex =  (prefixIndex2==-1||suffixIndex2==-1? Math.max(prefixIndex2,suffixIndex2):Math.min(prefixIndex2, suffixIndex2) );
        if(nextFixIndex>-1){
            if (isPush) {
                Cons astList = Cons.newInstance(r);
                String subExpStr = nextStr.substring(0, nextFixIndex);
                exp2List(subExpStr).forEach(astList::add);
                r.add(astList);
                return  parse(tempExpStr.substring(nextFixIndex+1), astList);
            } else {
                String subExpStr = nextStr.substring(0, nextFixIndex);
                exp2List(subExpStr).forEach(r.parent()::add);
                return parse(tempExpStr.substring(nextFixIndex+1), r.parent());
            }
        }else{
            return r;
        }
    }

    private static List<Object> exp2List(String exp){
        return exp.trim().length()==0? Collections.emptyList(): Arrays.stream(exp.trim().split(" ")).map(Parse::parseObj).collect(Collectors.toList());
    }


    private static String format(String str) {
        return str.contains("  ") ? format(str.replaceAll("  ", " ")) : str;
    }

    private static String replaceSpace(String str){
        if(str.contains("'")){
           int first =  str.indexOf("'");
           int last = str.substring(first+1).indexOf("'")+first+1;
            String substring = str.substring(first, last + 1);
            substring = substring.replaceAll(" ","\\\\\\O");
            return str.substring(0, first) + substring + replaceSpace(str.substring(last + 1));
        }
        return str;
    }

    private static Object parseObj(String atom){
        try {
            return Integer.valueOf(atom);
        } catch (NumberFormatException e) {
            if (atom.equals("true") || atom.equals("false")) {
                return Boolean.valueOf(atom);
            } else if (atom.indexOf("'") == 0 && atom.lastIndexOf("'") == atom.length() - 1) {
                return Strings.of(atom.replaceAll("'", "").replaceAll("\\\\O", " "));
            } else {
                return Symbols.of(atom);
            }
        }
    }
}
