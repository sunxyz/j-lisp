package org.yangrd.lab.lisp.support;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {

    public static String readFile(String file){
        StringBuilder buffer = new StringBuilder();
        file = file.indexOf("/")==0?file:System.getProperty("user.dir")+"/"+file;
        try ( FileReader f = new FileReader(file);BufferedReader reader = new BufferedReader(f)) {
            String line = reader.readLine();
            while (line != null) { // 如果 line 为空说明读完了
                buffer.append(line); // 将读到的内容添加到 buffer 中
                buffer.append("\n"); // 添加换行符
                line = reader.readLine(); // 读取下一行
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
       return buffer.toString();
    }
}
