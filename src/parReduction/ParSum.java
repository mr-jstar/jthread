/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parReduction;

/**
 *
 * @author jstar
 */
public class ParSum extends Thread{
    private int [] v;
    private int start;
    private int wend;
    private double sum;
    
    public ParSum(int [] v, int start, int wend ) {
        this.v = v;
        this.start = start;
        this.wend = wend;
    }
    
    @Override
    public void run() {
        System.out.println( start + "-" + wend + " startuje");
        sum = 0.0;
        for( int i = start; i < wend; i++ )
            sum+= v[i];
        System.out.println( start + "-" + wend + " skończył");
    }
    
    public double getResult() {
        return sum;
    }
    
}
