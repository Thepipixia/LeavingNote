package com.csj.ThreadLeaning;

public class Test {
    public static void main(String[] args) {
        Thread thread = new Thread(new Son());
        thread.start();
    }
}

class Son extends Father {
    @Override
    void execute() {

    }
}

abstract class Father extends GetClass implements Runnable{

    abstract void execute();
    @Override
    public void run() {
        myGetClass();
        execute();
    }
}

class GetClass <T> {
    public void myGetClass() {
        System.out.println(getClass());
    }
}