package parReduction;


/**
 *
 * @author jstar
 */
public class ParRedQ {

    public static double suma(int[] t) {
        double s = 0;
        for (int i = 0; i < t.length; i++) {
            s += t[i];
        }
        return s;
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1000000;
        int[] t = new int[n];
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < n; i++) {
            t[i] = r.nextInt(1000);
        }
        double s = suma(t);
        System.out.println(s / n);

        int lw = args.length > 1 ? Integer.parseInt(args[1]) : 4;
        ParQueue<Double> q = new ParQueue<>();
        // Tworzymy wątki
        ParSumQ[] w = new ParSumQ[lw];
        int perThread = n / lw;
        int rest = n % lw;
        for (int i = 0; i < rest; i++) {
            w[i] = new ParSumQ(t, i*(perThread+1), (i+1)*(perThread+1), q);
        }        
        for (int i = rest; i < lw; i++) {
            w[i] = new ParSumQ(t, rest*(perThread+1)+(i-rest)*perThread, rest*(perThread+1)+(i-rest+1)*perThread, q);
        }
        
        // Uruchamiamy je
        for (ParSumQ cw : w) {
            cw.start();
        }

        try {
            // Sumujemy wynik cząstkowe
            s = 0;
            for (int i = 0; i < lw; i++) {
                s += q.get();
            }
            System.out.println(s / n);
        } catch (InterruptedException e) {
            System.err.println("Przerwanie w trakcie oczekiwania na wyniki cząstkowe");
        }
    }
}
