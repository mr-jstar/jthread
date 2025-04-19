/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parIterative;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 *
 * @author jstar
 */
public class IterativeSolversImplementation {

    // Klasyczny, prosty algorytm Jacobiego
    public static int jacobi(SparseMatrix mA, double[] b, double[] x0, int maxIt, double eps, double[] x) {
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        for (int it = 0; it < maxIt; it++) {
            mA.multiply4IS(x, xp, b, 0, n);
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
    public static int parallel_jacobiA(SparseMatrix mA, double[] b, double[] x0, int maxIt, double eps, double[] x, int nthreads) {
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
    public static int parallel_jacobiB(SparseMatrix mA, double[] b, double[] x0, int maxIt, double eps, double[] x, int nthreads) {
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

    private static class JacobiPartSolverA extends Thread {

        private final SparseMatrix mA;
        private final double[] b;
        private final double eps;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] threadFinished;
        private final int myNumber;
        private final CyclicBarrier firstBarrier, secondBarrier;

        JacobiPartSolverA(SparseMatrix mA, double[] b, double eps, double[] x, double[] xp,
                int start, int finish, boolean[] threadFinished, int myNumber, CyclicBarrier bar1, CyclicBarrier bar2) {
            this.mA = mA;
            this.b = b;
            this.eps = eps;
            this.x = x;
            this.xp = xp;
            this.start = start;
            this.finish = finish;
            this.threadFinished = threadFinished;
            this.myNumber = myNumber;
            this.firstBarrier = bar1;
            this.secondBarrier = bar2;
        }

        @Override
        public void run() {
            int n = mA.size();
            System.err.println("Thread #" + myNumber + " will solve <" + start + "," + finish + ")");
            while (!threadFinished[myNumber]) {
                //System.err.println( "Thread#" + myNumber + " it#" + it++);
                mA.multiply4IS(x, xp, b, start, finish);
                double err = 0.0; // Liczymy błąd lokalnie i decydujemy, czy mamy dosyć
                for (int r = start; r < finish; r++) {
                    err += (x[r] - xp[r]) * (x[r] - xp[r]);
                }
                if (Math.sqrt(err * n / (finish - start)) <= eps) {  // Estymacja całego błędu na postawie fragmentu
                    threadFinished[myNumber] = true;  // Zapisujemy swój sygnał zakończenia - główny może to zmienić
                }
                System.arraycopy(x, start, xp, start, (finish - start)); // uaktualniamy "swój" kawałek xp
                try {
                    firstBarrier.await();  // Czekamy aż główny odbierze
                    secondBarrier.await(); // Czakamy aż główny zdecyduje
                    //System.err.println( "Thread#" + myNumber + " after secondBarrier");
                } catch (InterruptedException ex1) {
                    //System.err.println("Thread #" + myNumber + " interrupted while waiting at barrier");
                } catch (BrokenBarrierException ex2) {
                    //System.err.println("Thread #" + myNumber + " reporting broken barrier");
                }
            }
        }
    }

    private static class JacobiPartSolverB extends Thread {

        private final SparseMatrix mA;
        private final double[] b;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] theEnd;
        private final CyclicBarrier firstBarrier, secondBarrier;

        JacobiPartSolverB(SparseMatrix mA, double[] b, double[] x, double[] xp,
                int start, int finish, boolean[] theEnd, int myNumber, CyclicBarrier bar1, CyclicBarrier bar2) {
            this.mA = mA;
            this.b = b;
            this.x = x;
            this.xp = xp;
            this.start = start;
            this.finish = finish;
            this.theEnd = theEnd;
            this.firstBarrier = bar1;
            this.secondBarrier = bar2;
        }

        @Override
        public void run() {
            int n = mA.size();
            //System.err.println("Thread #" + myNumber + " will solve <" + start + "," + finish + ")");
            while (!theEnd[0]) {
                //System.err.println("Thread#" + myNumber + " it#" + it++);
                mA.multiply4IS(x, xp, b, start, finish);
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
            }
            //System.err.println("Thread#" + myNumber + " finished");
        }
    }

    public static int gaussSeidel(SparseMatrix mA, double[] b, double[] x0, int maxIt, double eps, double[] x) {
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        for (int it = 0; it < maxIt; it++) {
            mA.multiply4IS(x, xp, b, 0, n);
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
    // Wątek główny wykonuje nadrelaksację. Ustawienie omega == 1 wyłączy ją
    public static int parallel_GaussSeidel_SOR(SparseMatrix mA, double[] b, double[] x0, int maxIt, double omega, double eps, double[] x, int nthreads) {
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
                // SOR
                for (int i = 0; i < n; i++) {
                    x[i] = (1 - omega) * xp[i] + omega * x[i];
                }
                for (int i = 0; i < n; i++) {
                    err += (xp[i] - x[i]) * (xp[i] - x[i]);
                }
                //System.err.println("Gauss-Seidel (SOR): it#" + it + " err=" + Math.sqrt(err));
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

        private final SparseMatrix mA;
        private final double[] b;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] theEnd;
        private final CyclicBarrier firstBarrier, secondBarrier;
        private final int myNumber;  // for diagnostics - normally not used

        GaussSeidelPartSolver(SparseMatrix mA, double[] b, double[] x, double[] xp,
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
                mA.multiply4IS(x, x, b, start, finish);
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
}
