package parReduction;

/**
 *
 * @author jstar
 */
public class ParSumQ extends Thread {

    private final int[] v;
    private final int start;
    private final int wend;
    private final ParQueue<Double> q;

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
