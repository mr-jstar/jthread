/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parJacobi;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 *
 * @author jstar
 */
public class GaussSeidel {

    // Klasyczny, prosty algorytm Gaussa-Seidla
    public static int gaussSeidel(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double eps, double[] x) {
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        for (int it = 0; it < maxIt; it++) {
            for (int row = 0; row < n; row++) {
                x[row] = b[row];
                for (int col = 0; col < row; col++) {
                    x[row] -= mA.get(row, col) * x[col];
                }
                for (int col = row + 1; col < n; col++) {
                    x[row] -= mA.get(row, col) * x[col];
                }
                x[row] /= mA.get(row, row);
            }
            double err = 0.0;
            for (int row = 0; row < n; row++) {
                err += (x[row] - xp[row]) * (x[row] - xp[row]);
            }
            if (Math.sqrt(err) <= eps) {
                return it;
            }
            System.arraycopy(x, 0, xp, 0, n);
        }
        return maxIt;
    }

    // Zrównoleglenie wykorzystujące wątki
    // Tylko wątek główny monitoruje zbiezność i zatrzymuje wątki robocze przez flagę theEnd
    public static int parallel_GaussSeidel(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double eps, double[] x, int nthreads) {
        GaussSeidelPartSolver[] threads = new GaussSeidelPartSolver[nthreads];
        boolean[] theEnd = new boolean[1];
        theEnd[0] = false;
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        System.arraycopy(x0, 0, x, 0, n);
        int perThread = n / nthreads;
        int rest = n % nthreads;
        CyclicBarrier bar = new CyclicBarrier(nthreads + 1);
        CyclicBarrier bar2 = new CyclicBarrier(nthreads + 1);
        for (int i = 0; i < rest; i++) {
            threads[i] = new GaussSeidelPartSolver(mA, b, x, xp, i * (perThread + 1), (i + 1) * (perThread + 1), theEnd, i, bar, bar2);
            threads[i].start();
        }
        int base = rest * (perThread + 1);
        for (int i = rest; i < nthreads; i++) {
            threads[i] = new GaussSeidelPartSolver(mA, b, x, xp, base + (i - rest) * perThread, base + (i - rest + 1) * perThread, theEnd, i, bar, bar2);
            threads[i].start();
        }
        int it;
        for (it = 0; it < maxIt; it++) {
            try {
                bar.await();
                double err = 0.0;
                for (int i = 0; i < n; i++) {
                    err += (xp[i] - x[i]) * (xp[i] - x[i]);
                }
                System.err.println( "It #" + it + " err=" + Math.sqrt(err) );
                if (Math.sqrt(err) > eps && it < maxIt-1) {
                    theEnd[0] = false;
                    //System.err.println("All workers should continue.");
                    bar2.await();
                } else {
                    theEnd[0] = true;
                    //System.err.println("All workers should stop.");
                    bar2.await();
                    break;
                }
                //System.err.println( "Main after bar2" );
            } catch (InterruptedException ex1) {
                System.err.println("parallel_GaussSeidel interrupted while waiting at barrier");
            } catch (BrokenBarrierException ex2) {
                System.err.println("parallel_GaussSeidel reporting broken barrier");
            }
        }
        try {
            for (int i = 0; i < nthreads; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            System.err.println("JacobiParallel2 interrupted while waiting for workers");
        }
        return it;
    }

    static class GaussSeidelPartSolver extends Thread {

        private final SparseMatrixInterface mA;
        private final double[] b;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] theEnd;
        private final CyclicBarrier firstBarrier, secondBarrier;
        private final int myNumber;

        GaussSeidelPartSolver(SparseMatrixInterface mA, double[] b, double[] x, double[] xp,
                int start, int finish, boolean[] theEnd, int myNumber, CyclicBarrier bar1, CyclicBarrier bar2) {
            this.mA = mA;
            this.b = b;
            this.x = x;
            this.xp = xp;
            this.start = start;
            this.finish = finish;
            this.theEnd = theEnd;
            this.myNumber = myNumber;
            this.firstBarrier = bar1;
            this.secondBarrier = bar2;
        }

        @Override
        public void run() {
            int n = mA.size();
            //System.err.println("Thread #" + myNumber + " will solve <" + start + "," + finish + ")");
            while (!theEnd[0]) {
                //System.err.println("Thread#" + myNumber + " it#" + it++);
                for (int row = start; row < finish; row++) {
                    x[row] = b[row];
                    for (int col = 0; col < n; col++) {
                        x[row] -= mA.get(row, col) * x[col];
                    }
                    x[row] /= mA.get(row, row);
                }
                try {
                    firstBarrier.await();  // Czekamy aż wszyscy skończą
                    secondBarrier.await(); // Czakamy aż główny zdecyduje, co dalej
                    if (!theEnd[0]) {
                        //System.err.println("Thread#" + myNumber + " copying x -> xp");
                        System.arraycopy(x, start, xp, start, (finish - start)); // uaktualniamy "swój" kawałek xp
                    }
                    //System.err.println( "Thread#" + myNumber + " after secondBarrier");
                } catch (InterruptedException ex1) {
                    //System.err.println("Thread #" + myNumber + " interrupted while waiting at barrier");
                } catch (BrokenBarrierException ex2) {
                    //System.err.println("Thread #" + myNumber + " reporting broken barrier");
                }
                //System.err.println("Thread#" + myNumber + " finished");
            }
        }
    }

    public static void main_NOT_READY(String[] args) {
        System.err.println( "NOT READY YET!!!"); System.exit(0);
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        System.out.println("Buduję rzadki układ " + n + " równań.");
        SparseMatrixInterface mA = new SparseMatrix(n);
        Random rnd = new Random();
        for (int row = 0; row < n; row++) {
            for (int t = 0; t < n / 10; t++) {
                int col = rnd.nextInt(n);
                mA.set(row, col, rnd.nextDouble());
            }
            mA.set(row, row, (1.0 + rnd.nextDouble()) * n);
        }
        double[] b = new double[n];
        for (int row = 0; row < n; row++) {
            b[row] = 0.0;
            for (int col = 0; col < n; col++) {
                b[row] += col * mA.get(row, col);
            }
        }
        double[] x0 = new double[n];
        for (int r = 0; r < n; r++) {
            x0[r] = 0.0;
        }
        double[] x = new double[n];

        System.out.println("Rozwiązuję:");
        System.out.flush();

        long start = System.nanoTime();
        System.out.println("Sekwencyjnie:\n\tWykonano " + gaussSeidel(mA, b, x0, n, 1e-9, x) + " iteracji");
        long end = System.nanoTime();
        long duration = end - start;
        System.out.println("\tCzas wykonania: " + String.format("%.3f", duration / 1e6) + " milisekund");

        if (n < 20) {
            for (int r = 0; r < n; r++) {
                System.out.println(x[r]);
            }
        } else {
            for (int r = 0; r < n; r++) {
                if (Math.abs(x[r] - r) > 1e-3) {
                    System.out.println(x[r] + " mi się nie podoba!");
                }
            }
        }

        double[] xp = new double[n];
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.println("Współbieżnie:");
        System.out.println("\t" + cores + " wątk" + (cores < 5 ? "i" : "ów") + ":");
        start = System.nanoTime();
        System.out.println("\t\tWykonano " + parallel_GaussSeidel(mA, b, x0, n, 1e-9, xp, cores) + " iteracji");
        end = System.nanoTime();
        duration = end - start;
        System.out.println("\t\tCzas wykonania: " + String.format("%.3f", duration / 1e6) + " milisekund");
        System.out.println("\t" + cores / 2 + " wątk" + (cores / 2 < 5 ? "i" : "ów" + ":"));
        start = System.nanoTime();
        System.out.println("\t\tWykonano " + parallel_GaussSeidel(mA, b, x0, n, 1e-9, xp, cores / 2) + " iteracji");
        end = System.nanoTime();
        duration = end - start;
        System.out.println("\t\tCzas wykonania: " + String.format("%.3f", duration / 1e6) + " milisekund");
        if (n < 20) {
            for (int r = 0; r < n; r++) {
                System.out.println(xp[r]);
            }
        } else {
            for (int r = 0; r < n; r++) {
                if (Math.abs(xp[r] - r) > 1e-6) {
                    System.out.println(xp[r] + " mi się nie podoba!");
                }
            }
        }
    }
}
