/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parJacobi;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class JacobiPartSolverA extends Thread {

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
        System.err.println( "Thread #" + myNumber + " will solve <" + start + "," + finish + ")");
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
            if (Math.sqrt(err) <= eps) {
                threadFinished[myNumber] = true;  // Główny może to zmienić
            }
            System.arraycopy(x, start, xp, start, (finish - start));
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
};
