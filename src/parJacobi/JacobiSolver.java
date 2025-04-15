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
public class JacobiSolver {

    // Klasyczny, prosty algorytm Jacobiego
    public static int jacobi(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double eps, double[] x) {
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        for (int it = 0; it < maxIt; it++) {
            for (int row = 0; row < n; row++) {
                x[row] = -b[row];
                for (int col = 0; col < n; col++) {
                    x[row] += mA.get(row, col) * xp[col];
                }
                x[row] = xp[row] - x[row] / mA.get(row, row);
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
    // Każdy wątek monitoruje lokalnie zbiezność => patrz klasa JacobiSolverA
    public static int parallel_jacobiA(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double eps, double[] x, int nthreads) {
        JacobiPartSolverA[] threads = new JacobiPartSolverA[nthreads];
        boolean[] threadFinished = new boolean[nthreads];
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        int perThread = n / nthreads;
        int rest = n % nthreads;
        CyclicBarrier bar = new CyclicBarrier(nthreads + 1);
        CyclicBarrier bar2 = new CyclicBarrier(nthreads + 1);
        for (int i = 0; i < rest; i++) {
            threads[i] = new JacobiPartSolverA(mA, b, eps / nthreads, x, xp, i * (perThread + 1), (i + 1) * (perThread + 1), threadFinished, i, bar, bar2);
            threadFinished[i] = false;
            threads[i].start();
        }
        int base = rest * (perThread + 1);
        for (int i = rest; i < nthreads; i++) {
            threads[i] = new JacobiPartSolverA(mA, b, eps / nthreads, x, xp, base + (i - rest) * perThread, base + (i - rest + 1) * perThread, threadFinished, i, bar, bar2);
            threadFinished[i] = false;
            threads[i].start();
        }
        int it;
        for (it = 0; it < maxIt; it++) {
            try {
                bar.await();
                //System.err.println( "It #" + it );
                boolean allFinished = true;  // sprawdzamy, czy wszyscy chcą skoĶńczyć
                for (int i = 0; i < nthreads; i++) {
                    allFinished = allFinished && threadFinished[i];
                }
                if (it == maxIt - 1) {
                    allFinished = true;
                    for (int i = 0; i < nthreads; i++) {
                        threadFinished[i] = true;
                    }
                }
                //printBool( threadFinished )
                if (!allFinished) {
                    for (int i = 0; i < nthreads; i++) {
                        threadFinished[i] = false;
                    }
                    //System.err.println( "Main before bar2" );
                    bar2.await();
                } else {
                    bar2.await();
                    break;
                }
                //System.err.println( "Main after bar2" );
            } catch (InterruptedException ex1) {
                System.err.println("JacobiParallelA interrupted while waiting at barrier");
            } catch (BrokenBarrierException ex2) {
                System.err.println("JacobiParallelA reporting broken barrier");
            }
        }
        return it;
    }

    // Zrównoleglenie wykorzystujące wątki
    // Tylko wątek główny monitoruje zbiezność i zatrzymuje wątki robocze przez flagę theEnd
    // => patrz klasa JacobiSolverB
    public static int parallel_jacobiB(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double eps, double[] x, int nthreads) {
        JacobiPartSolverB[] threads = new JacobiPartSolverB[nthreads];
        boolean[] theEnd = new boolean[1];
        theEnd[0] = false;
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        int perThread = n / nthreads;
        int rest = n % nthreads;
        CyclicBarrier bar = new CyclicBarrier(nthreads + 1);
        CyclicBarrier bar2 = new CyclicBarrier(nthreads + 1);
        for (int i = 0; i < rest; i++) {
            threads[i] = new JacobiPartSolverB(mA, b, x, xp, i * (perThread + 1), (i + 1) * (perThread + 1), theEnd, i, bar, bar2);
            threads[i].start();
        }
        int base = rest * (perThread + 1);
        for (int i = rest; i < nthreads; i++) {
            threads[i] = new JacobiPartSolverB(mA, b, x, xp, base + (i - rest) * perThread, base + (i - rest + 1) * perThread, theEnd, i, bar, bar2);
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
                //System.err.println( "It #" + it + " err=" + Math.sqrt(err) );
                if (Math.sqrt(err) > eps && it < maxIt - 1) {
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
                System.err.println("JacobiParallelB interrupted while waiting at barrier");
            } catch (BrokenBarrierException ex2) {
                System.err.println("JacobiParallelB reporting broken barrier");
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

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
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
        System.out.println("Sekwencyjnie:\n\tWykonano " + jacobi(mA, b, x0, n, 1e-9, x) + " iteracji");
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
        int []p;
        if( args.length > 1 ) {
            p = new int[args.length-1];
            for( int i= 1; i < args.length; i++ )
                p[i-1] = Integer.parseInt(args[i]);
        } else {
            p = new int[2];
            p[0] = cores/2;
            p[1] = cores;
        }
        System.out.println("Współbieżnie:");
        for( int nThreads : p ) {
            System.out.println("\t" + nThreads + " wątk" + (nThreads < 5 ? "i" : "ów") + ":");
            start = System.nanoTime();
            System.out.println("\t\tWykonano " + parallel_jacobiB(mA, b, x0, n, 1e-9, xp, nThreads) + " iteracji");
            end = System.nanoTime();
            duration = end - start;
            System.out.println("\t\tCzas wykonania: " + String.format("%.3f", duration / 1e6) + " milisekund");
        }
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
