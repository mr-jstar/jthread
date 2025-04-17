/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parIterative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jstar
 */
public class SparseMatrix implements SparseMatrixInterface {

    private HashMap<Integer, HashMap<Integer, Double>> a = new HashMap<>();
    private int n;

    public SparseMatrix(int n) {
        this.n = n;
    }

    @Override
    public void set(int i, int j, double v) {
        if (!a.containsKey(i)) {
            a.put(i, new HashMap<>());
        } else {
            if (a.get(i).containsKey(j)) {
                a.get(i).remove(j);
            }
        }
        a.get(i).put(j, v);
    }

    @Override
    public double get(int i, int j) {
        if (!a.containsKey(i)) {
            return 0.0;
        }
        if (a.get(i).containsKey(j)) {
            return a.get(i).get(j);
        } else {
            return 0.0;
        }
    }

    @Override
    public int size() {
        return n;
    }

    @Override
    public void multiply4IS(double[] x, double [] xp, double[] b, int start, int end) {
        double sum, aii= 0.0;
        for (int row = start; row < end; row++) {
            sum = b[row];
            for( Map.Entry<Integer, Double> e : a.get(row).entrySet() ) {
                if( e.getKey() < row )
                    sum -= e.getValue() * x[e.getKey()];
                else if( e.getKey() > row )
                    sum -= e.getValue() * xp[e.getKey()];
                else
                    aii = e.getValue();
                    
            }
            x[row] = sum / aii;
        }
    }

    public CRS toCRS() {
        int[] ia = new int[n + 1];
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> rowEntry : a.entrySet()) {
            int i = rowEntry.getKey();
            ia[i + 1] = rowEntry.getValue().size();
        }
        for (int i = 2; i <= n; i++) {
            ia[i] += ia[i - 1];
        }
        int nz = ia[n];
        int[] ja = new int[nz];
        double[] crsa = new double[nz];
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> rowEntry : a.entrySet()) {
            int i = rowEntry.getKey();
            List<Map.Entry<Integer, Double>> row = new ArrayList<>(rowEntry.getValue().entrySet());

            Collections.sort(row, new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> e1, Map.Entry<Integer, Double> e2) {
                    return e1.getKey() - e2.getKey();
                }
            });
            int j = ia[i];
            for (Map.Entry<Integer, Double> e : row) {
                ja[j] = e.getKey();
                crsa[j] = e.getValue();
                j++;
            }
        }

        return new CRS(ia, ja, crsa);
    }
}
