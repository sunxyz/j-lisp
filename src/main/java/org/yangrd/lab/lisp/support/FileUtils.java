package org.yangrd.lab.lisp.support;

import org.yangrd.lab.lisp.type.Booleans;
import org.yangrd.lab.lisp.type.Nil;
import org.yangrd.lab.lisp.type.Strings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Function;

public class FileUtils {

    public static String readFile(String file){
        StringBuilder buffer = new StringBuilder();
        file = file.indexOf("/")==0?file:System.getProperty("user.dir")+"/"+file;
        try ( FileReader f = new FileReader(file);BufferedReader reader = new BufferedReader(f)) {
            String line = reader.readLine();
            // 如果 line 为空说明读完了
            while (line != null) {
                line = line.trim();
                line = line.indexOf("//")==0||line.indexOf(";")==0?"":line;
                // 将读到的内容添加到 buffer 中
                buffer.append(line);
                // 添加换行符
                buffer.append("\n");
                // 读取下一行
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
       return buffer.toString();
    }

    public static void readFileLine(String file, Function<BufferedReader, Nil> fReader){
        file = file.indexOf("/")==0?file:System.getProperty("user.dir")+"/"+file;
        try ( FileReader f = new FileReader(file);BufferedReader reader = new BufferedReader(f)) {
            fReader.apply(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
