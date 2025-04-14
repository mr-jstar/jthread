/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parReduction;

import java.util.Random;

/**
 *
 * @author jstar
 */
public class ParQueue<T> {

    private Object[] t = new Object[32];
    private int n = 0;

    public synchronized void put(T x) {
        if (n == t.length) {
            doubleSize();
        }
        t[n++] = x;
        notify();
    }

    public synchronized T get() throws InterruptedException {
        while (n == 0) {
            wait();
        }
        @SuppressWarnings("unchecked")
        T ret = (T)t[0];
        moveAll();
        return ret;
    }

    public boolean isEmpty() {
        return n == 0;
    }

    private void doubleSize() {
        Object[] nt = new Object[2 * t.length];
        System.arraycopy(t, 0, nt, 0, n);
        t = nt;
    }

    private void moveAll() {
        /*
        for (int i =1; i< n-1; i++ )
            t[i - 1] = t[i];
         */
        System.arraycopy(t, 1, t, 0, n - 1);
        n--;
    }

    public static void main(String[] args) {
        ParQueue<Double> q = new ParQueue<>();
        int delay = 1000;

        System.out.println("Feeder");
        MyFeeder feeder = new MyFeeder(delay, q);
        feeder.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.err.println("Main przerwany gdy czekał na uruchomienie eatera");
        }

        System.out.println("Eater");
        MyEater eater = new MyEater(q);
        eater.setDaemon(true);
        eater.start();

        try {
            Thread.sleep(5000);
            feeder.canRun = false;
        } catch (InterruptedException e) {
            System.err.println("Main przerwany gdy czekał na uruchomienie eatera");
        }

        try {
            feeder.join();
            System.out.println("Koniec");
        } catch (InterruptedException e) {
            System.err.println("Main przerwany gdy czekał aż feeder skończy");
        }

    }

    static class MyEater extends Thread {

        ParQueue<Double> q;

        MyEater(ParQueue<Double> q) {
            this.q = q;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    System.out.println("Eater: " + q.get());
                }
            } catch (InterruptedException e) {
                System.err.println("Eater przerwany");
            }
        }
    }

    static class MyFeeder extends Thread {

        public boolean canRun;
        ParQueue<Double> q;
        int delay;

        MyFeeder(int delay, ParQueue<Double> q) {
            this.delay = delay;
            this.q = q;
        }

        @Override
        public void run() {
            canRun = true;
            try {
                Random r = new Random();
                while (canRun) {
                    double x = r.nextDouble() * 100;
                    System.out.println("Feeder -> " + x);
                    q.put(x);
                    sleep(delay);
                }
            } catch (InterruptedException e) {
                System.err.println("Feeder przerwany!");
            }
        }
    }
}
