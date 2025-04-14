/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parJacobi;

/**
 *
 * @author jstar
 */
public interface SparseMatrixInterface {

    double get(int i, int j);

    void set(int i, int j, double v);
    
    int size();
    
}
