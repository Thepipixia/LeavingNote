package com.csj.JVMLearning.Class;

public class Initialize {
    public static void main(String[] args) throws ClassNotFoundException {
        System.out.println(InitClass1.a);

        System.out.println("============");

        System.out.println(InitClass1.a);

        System.out.println("============");

        Class<?> aClass = Class.forName("com.csj.JVMLearning.Class.InitClass3");

        System.out.println("============");

        Class<?> aClass1 = Class.forName("com.csj.JVMLearning.Class.IniteClass4", false, aClass.getClassLoader());

        System.out.println("============");

        InitClass5 class5 = new InitClass5();
    }
}

class InitClass1 {
    static {
        System.out.println("Class初始化了");
    }
    public static int a = 1;
}

class InitClass2 {
    static {
        System.out.println("Class2初始化了");
    }
    public static final int a = 1;
}

class InitClass3 {
    static {
        System.out.println("Class3初始化了");
    }
}

class IniteClass4 {
    static {
        System.out.println("Class4初始化了");
    }
    public static int a = 1;
}

class InitClass5 {
    static {
        System.out.println("Class5初始化了");
    }
}
class InitClass6 {
    public static int a = 1;

    static {
        System.out.println("Class6初始化了");
    }

}