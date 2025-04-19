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
public class HashMapSparseMatrix implements SparseMatrix {

    private HashMap<Integer, HashMap<Integer, Double>> matrix = new HashMap<>();
    private int n;

    public HashMapSparseMatrix(int n) {
        this.n = n;
    }

    @Override
    public void set(int i, int j, double v) {
        if (!matrix.containsKey(i)) {
            matrix.put(i, new HashMap<>());
        } else {
            if (matrix.get(i).containsKey(j)) {
                matrix.get(i).remove(j);
            }
        }
        matrix.get(i).put(j, v);
    }

    @Override
    public double get(int i, int j) {
        if (!matrix.containsKey(i)) {
            return 0.0;
        }
        if (matrix.get(i).containsKey(j)) {
            return matrix.get(i).get(j);
        } else {
            return 0.0;
        }
    }

    @Override
    public int size() {
        return n;
    }
    
    @Override
    public double[] multiply(double[] x, int start, int end) {
        double[] result = new double[end-start];
        for (int row = start; row < end; row++) {
            result[row] = 0.0;
            for (Map.Entry<Integer, Double> e : matrix.get(row).entrySet()) {
                int col = e.getKey();
                result[row] += e.getValue() * x[col];
            }
        }
        return result;
    }

    @Override
    public void multiply4IS(double[] x, double[] xp, double[] b, int start, int end) {
        double sum, aii = 0.0;
        for (int row = start; row < end; row++) {
            sum = b[row];
            for (Map.Entry<Integer, Double> e : matrix.get(row).entrySet()) {
                if (e.getKey() < row) {
                    sum -= e.getValue() * x[e.getKey()];
                } else if (e.getKey() > row) {
                    sum -= e.getValue() * xp[e.getKey()];
                } else {
                    aii = e.getValue();
                }

            }
            x[row] = sum / aii;
        }
    }

    public CRSSparseMatrix toCRS() {
        int[] ia = new int[n + 1];
        long start = System.nanoTime();
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            ia[i + 1] = rowEntry.getValue().size();
        }
        for (int i = 2; i <= n; i++) {
            ia[i] += ia[i - 1];
        }
        long end = System.nanoTime();
        long duration = end - start;
        //System.err.println("ia made in " + (duration / 1e6) + " ms.");
        int nz = ia[n];
        int[] ja = new int[nz];
        double[] a = new double[nz];
        start = System.nanoTime();
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> rowEntry : matrix.entrySet()) {
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
                a[j] = e.getValue();
                j++;
            }
        }
        end = System.nanoTime();
        duration = end - start;
        //System.err.println("ja & a made in " + (duration / 1e6) + " ms.");

        return new CRSSparseMatrix(ia, ja, a);
    }

    public CRSSparseMatrix toCRSfaster() {
        int[] ia = new int[n + 1];
        long start = System.nanoTime();
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            ia[i + 1] = rowEntry.getValue().size();
        }
        for (int i = 2; i <= n; i++) {
            ia[i] += ia[i - 1];
        }
        long end = System.nanoTime();
        long duration = end - start;
        //System.err.println("ia made in " + (duration / 1e6) + " ms.");
        int nz = ia[n];
        int[] ja = new int[nz];
        double[] a = new double[nz];
        start = System.nanoTime();
        for (HashMap.Entry<Integer, HashMap<Integer, Double>> rowEntry : matrix.entrySet()) {
            int i = rowEntry.getKey();
            int j = ia[i];
            for (Map.Entry<Integer, Double> e : rowEntry.getValue().entrySet()) {
                ja[j] = e.getKey();
                a[j] = e.getValue();
                j++;
            }
        }
        end = System.nanoTime();
        duration = end - start;
        //System.err.println("ja & a made in " + (duration / 1e6) + " ms.");

        return new CRSSparseMatrix(ia, ja, a);
    }
}
