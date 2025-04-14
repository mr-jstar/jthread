package parReduction;

/**
 *
 * @author jstar
 */
public class ParSumQ extends Thread {

    private int[] v;
    private int start;
    private int wend;
    private ParQueue<Double> q;

    public ParSumQ(int[] v, int start, int wend, ParQueue<Double> q) {
        this.v = v;
        this.start = start;
        this.wend = wend;
        this.q = q;
    }

    @Override
    public void run() {
        System.out.println(start + "-" + wend + " startuje");
        double sum = 0.0;
        for (int i = start; i < wend; i++) {
            sum += v[i];
        }
        q.put(sum);
        System.out.println(start + "-" + wend + " skończył");
    }

}
