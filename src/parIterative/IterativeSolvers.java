/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parIterative;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 *
 * @author jstar
 */
public class IterativeSolvers {

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

    private static class JacobiPartSolverA extends Thread {

        private final SparseMatrixInterface mA;
        private final double[] b;
        private final double eps;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] threadFinished;
        private final int myNumber;
        private final CyclicBarrier firstBarrier, secondBarrier;

        JacobiPartSolverA(SparseMatrixInterface mA, double[] b, double eps, double[] x, double[] xp,
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
                for (int row = start; row < finish; row++) {
                    x[row] = -b[row];
                    for (int col = 0; col < n; col++) {
                        x[row] += mA.get(row, col) * xp[col];
                    }
                    x[row] = xp[row] - x[row] / mA.get(row, row);
                }
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

        private final SparseMatrixInterface mA;
        private final double[] b;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] theEnd;
        private final CyclicBarrier firstBarrier, secondBarrier;

        JacobiPartSolverB(SparseMatrixInterface mA, double[] b, double[] x, double[] xp,
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
                for (int row = start; row < finish; row++) {
                    x[row] = -b[row];
                    for (int col = 0; col < n; col++) {
                        x[row] += mA.get(row, col) * xp[col];
                    }
                    x[row] = xp[row] - x[row] / mA.get(row, row);
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
            }
            //System.err.println("Thread#" + myNumber + " finished");
        }
    }

    public static int gaussSeidel(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double eps, double[] x) {
        int n = mA.size();
        double[] xp = new double[n];
        System.arraycopy(x0, 0, xp, 0, n);
        for (int it = 0; it < maxIt; it++) {
            for (int row = 0; row < n; row++) {
                double sum = b[row];
                for (int col = 0; col < row; col++) {
                    sum -= mA.get(row, col) * x[col];
                }
                for (int col = row + 1; col < n; col++) {
                    sum -= mA.get(row, col) * xp[col];
                }
                x[row] = sum / mA.get(row, row);
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
    // Wątek główny wykonuje nadrelaksację. Ustawienie omega == 1 wyłączy ją
    public static int parallel_GaussSeidel_SOR(SparseMatrixInterface mA, double[] b, double[] x0, int maxIt, double omega, double eps, double[] x, int nthreads) {
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

        private final SparseMatrixInterface mA;
        private final double[] b;
        private volatile double[] x;
        private volatile double[] xp;
        private final int start;
        private final int finish;
        private volatile boolean[] theEnd;
        private final CyclicBarrier firstBarrier, secondBarrier;
        private final int myNumber;  // for diagnostics - normally not used

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
                    double sum = b[row];
                    for (int col = 0; col < row; col++) {
                        sum -= mA.get(row, col) * x[col];
                    }
                    for (int col = row + 1; col < n; col++) {
                        sum -= mA.get(row, col) * x[col];
                    }
                    x[row] = sum / mA.get(row, row);
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

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        double omega = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;
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
        Arrays.fill(x0, 0.0);
        double[] x = new double[n];
        Arrays.fill(x, 0.0);
        System.out.println("Rozwiązuję:");
        System.out.flush();

        long start = System.nanoTime();
        int it = jacobi(mA, b, x0, n, 1e-9, x);
        long end = System.nanoTime();
        long duration = end - start;
        System.out.println("Metoda                                L.w.   #it     Czas [ms]      S       ε");
        System.out.println("-------------------------------------------------------------------------------");
        final String FMT = "%-35.35s    %2d    %3d    %10.3f   %5.2f   %5.2f";
        System.out.println(String.format(FMT, "Sekwencyjnie Jacobi", 1, it, duration / 1e6, 1.0, 1.0));
        double S1j = duration;

        Arrays.fill(x0, 0.0);
        Arrays.fill(x, 0.0);
        start = System.nanoTime();
        it = gaussSeidel(mA, b, x0, n, 1e-9, x);
        end = System.nanoTime();
        duration = end - start;
        System.out.println(String.format(FMT, "Sekwencyjnie Gauss-Seidel", 1, it, duration / 1e6, 1.0, 1.0));
        double S1gs = duration;

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
        int cores = Runtime.getRuntime().availableProcessors();
        int[] p;
        if (args.length > 2) {
            p = new int[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                p[i - 2] = Integer.parseInt(args[i]);
            }
        } else {
            p = new int[2];
            p[0] = cores / 2;
            p[1] = cores;
        }
        for (int nThreads : p) {
            Arrays.fill(x0, 0.0);
            Arrays.fill(x, 0.0);
            start = System.nanoTime();
            it = parallel_jacobiB(mA, b, x0, n, 1e-9, x, nThreads);
            end = System.nanoTime();
            duration = end - start;
            System.out.println(String.format(FMT, "Współbieżnie Jacobi", nThreads, it, duration / 1e6, S1j/duration, ((duration/S1j-1/nThreads)/(1-1/nThreads))));

            if (n < 20) {
                for (int r = 0; r < n; r++) {
                    System.out.println(x[r]);
                }
            } else {
                for (int r = 0; r < n; r++) {
                    if (Math.abs(x[r] - r) > 1e-6) {
                        System.out.println(x[r] + " mi się nie podoba!");
                    }
                }
            }
        }
        for (int nThreads : p) {
            Arrays.fill(x0, 0.0);
            Arrays.fill(x, 0.0);
            start = System.nanoTime();
            it = parallel_GaussSeidel_SOR(mA, b, x0, n, omega, 1e-9, x, nThreads);
            end = System.nanoTime();
            duration = end - start;
            System.out.println(String.format(FMT, "Współbieżnie Gauss-Seidel " + (omega < 1 ? "(SOR)" : "     "), nThreads, it, duration / 1e6, S1gs/duration, ((duration/S1gs-1/nThreads)/(1-1/nThreads))));

            if (n < 20) {
                for (int r = 0; r < n; r++) {
                    System.out.println(x[r]);
                }
            } else {
                for (int r = 0; r < n; r++) {
                    if (Math.abs(x[r] - r) > 1e-6) {
                        System.out.println(x[r] + " mi się nie podoba!");
                    }
                }
            }
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("Koniec.");
        if( args.length == 0 )
            System.out.println("""
                               \n\n
                               Przy uruchomieniu klasy możesz podać argumenty, które oznaczają kolejno:
                               \t - wielkość układu równań
                               \t - współczynnik nadrelaksacji (omega) dla SOR:  1 -- bez SOR, < 1-- z S)R
                               \t - listę liczb wątków, które mają być uruchomione w algorytmach równoległych
                               
                               Np.
                               java IterativeSolvers  5000 1 2 4 6 8 10 12 
                                uruchomi solvery dla układu 5000 równań, bez SOR, z użyciem kolejno 2,4,6..12 wątków.
                               """);
    }
}
