package com.csj.JVMLearning.Class;

import java.util.ArrayList;
import java.util.List;

public class Test {

    static List<Object> list = new ArrayList<>();
    static int count = 0;
    public static void main(String[] args) throws Exception {
        while (true) {
            System.in.read();
            if (count++ % 8 == 0) {
                System.out.println("======================");
                System.out.println("======================");
                System.out.println("======================");
                System.out.println("======================");
                System.out.println("======================");
                list.clear();
            }
            list.add(new byte[1024*500]);
        }
    }
}

