/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parIterative;

import java.util.HashMap;

/**
 *
 * @author jstar
 */
public class SparseMatrix implements SparseMatrixInterface {
    
    private HashMap<Integer,HashMap<Integer,Double>> a = new HashMap<>();
    private int n;

    public SparseMatrix( int n) {
        this.n = n;
    }

    @Override
    public void set(int i, int j, double v) {
        if( ! a.containsKey(i))
            a.put(i, new HashMap<>());
        else {
            if( a.get(i).containsKey(j))
                a.get(i).remove(j);
        }
        a.get(i).put(j, v);
    }

    @Override
    public double get(int i, int j) {
        if( ! a.containsKey(i) )
            return 0.0;
        if( a.get(i).containsKey(j) )
            return a.get(i).get(j);
        else
            return 0.0;
    }

    @Override
    public int size() {
        return n;
    }

}
