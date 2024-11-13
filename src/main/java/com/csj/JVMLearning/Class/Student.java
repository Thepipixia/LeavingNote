package com.csj.JVMLearning.Class;

import java.util.Arrays;

public class Student {
    public static void main(String[] args) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        System.out.println(Arrays.toString(stackTrace));
    }

}

class Main extends BaseJob{
    @Override
    public void execute() {
        System.out.println("execute");
    }
}

abstract class BaseJob extends AopProxyHolder<BaseJob> {
    public void run() {
        getProxy().execute();
    }

    public abstract void execute();
}


class AopProxyHolder<T> {

    public T getProxy() {

        System.out.println(getClass());
        return (T) new Object();
    }
}