package parIterative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jstar
 */
public class CRSSparseMatrix implements SparseMatrix {

    private int[] ia;
    private int[] ja;
    private double[] a;

    public CRSSparseMatrix(int[] ia, int[] ja, double[] a) {
        this.ia = ia;
        this.ja = ja;
        this.a = a;
    }

    public int nRows() {
        return ia.length - 1;
    }

    public int nCols() {
        return ia.length - 1;
    }
    
    public int nNonZeroes() {
        return ia[ia.length - 1];
    }

    @Override
    public double get(int i, int j) {
        for (int k = ia[i]; k < ia[i + 1]; k++) {
            if (ja[k] == j) {
                return a[k];
            }
        }
        return 0.0;
    }
    
    @Override
    public void set(int i, int j, double value ) {
        for (int k = ia[i]; k < ia[i + 1]; k++) {
            if (ja[k] == j) {
                a[k] = value;
                return;
            }
        }
        if( value != 0.0 )
            throw new IllegalArgumentException("CRS: No entry A[" + i + "," + j + "]");
    }
    
    public void clearRow( int i ) {
        for( int k = ia[i]; k < ia[i + 1]; k++ )
            a[k] = 0.0;
    }

    public void addTo(int i, int j, double d) {
        for (int k = ia[i]; k < ia[i + 1]; k++) {
            if (ja[k] == j) {
                a[k] += d;
                return;
            }
        }
        throw new IllegalArgumentException("CRS: No entry A[" + i + "," + j + "]");
    }

    @Override
    public double[] multiply(double[] x, int start, int end) {
        double[] r = new double[end-start];
        for (int i = start; i < end; i++) {
            for (int k = ia[i]; k < ia[i + 1]; k++) {
                r[i] += x[ja[k]] * a[k];
            }
        }
        return r;
    }
    
    @Override
    public void multiply4IS(double[] x, double [] xp, double [] b, int start, int end) {
        double sum, aii= 0.0;
        for (int i = start; i < end; i++) {
            sum = b[i];
            for (int k = ia[i]; k < ia[i + 1]; k++) {
                if( ja[k] < i )
                    sum -= x[ja[k]] * a[k];
                else if( ja[k] > i )
                    sum -= xp[ja[k]] * a[k];
                else
                    aii = a[k];
            }
            x[i] = sum / aii;
        }
    }
    
    public void multiply(double[] x, double[] r) {
        for (int i = 0; i < ia.length - 1; i++) {
            r[i] = 0.0;
            for (int k = ia[i]; k < ia[i + 1]; k++) {
                r[i] += x[ja[k]] * a[k];
            }
        }
    }

    public SparseMatrix transpose() { 
        int rows = ia.length - 1;
        int cols = rows;
        int[] iaT = new int[cols + 1];
        int nnz = a.length;
        int[] jaT = new int[nnz];
        double[] aT = new double[nnz];

        // Obliczanie liczby elementów w każdej kolumnie macierzy
        for (int i = 0; i < nnz; i++) {
            iaT[ja[i] + 1]++;
        }

        // Przekształcenie liczników w indeksy
        for (int i = 0; i < cols; i++) {
            iaT[i + 1] += iaT[i];
        }

        // Wypełnianie jaT i aT
        for (int i = 0; i < rows; i++) {
            for (int j = ia[i]; j < ia[i + 1]; j++) {
                int col = ja[j];
                int dest = iaT[col];

                jaT[dest] = i;
                aT[dest] = a[j];
                iaT[col]++;
            }
        }

        // Przywrócenie poprawnych wartości iaT
        for (int i = cols; i > 0; i--) {
            iaT[i] = iaT[i - 1];
        }
        iaT[0] = 0;

        return new CRSSparseMatrix(iaT, jaT, aT);
    }
    
    public HashMapSparseMatrix toSparseMatrix() {
        HashMapSparseMatrix hsm = new HashMapSparseMatrix(ia.length-1);
        for (int i = 0; i < ia.length - 1; i++) {
            for (int k = ia[i]; k < ia[i + 1]; k++) {
                hsm.set( i, ja[k], a[k] );
            }
        }
        return hsm;
    }

    @Override
    public String toString() {
        int n = ia.length - 1;
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                ret.append(get(i, j)).append(" ");
            }
            ret.append("\n");
        }
        return ret.toString();
    }

    @Override
    public int size() {
        return ia.length-1;
    }
}
