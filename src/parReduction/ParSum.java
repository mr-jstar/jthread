package parReduction;

/**
 *
 * @author jstar
 */
public class ParSum extends Thread{
    private final int [] v;
    private final int start;
    private final int wend;
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
