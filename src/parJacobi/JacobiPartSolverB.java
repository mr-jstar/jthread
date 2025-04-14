/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parJacobi;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class JacobiPartSolverB extends Thread {

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
};
