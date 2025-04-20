package parReduction;

/**
 *
 * @author jstar
 */
public class ParRed {

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

        int lw = 5;
        // Tworzymy wątki
        ParSum[] w = new ParSum[lw];
        for (int i = 0; i < lw; i++) {
            w[i] = new ParSum(t, i * (n / lw), (i + 1) * (n / lw));
        }
        // Uruchamiamy je
        for (ParSum cw : w) {
            cw.start();
        }
        // Czekamy aż skończą
        for (ParSum cw : w) {
            try {
                cw.join();
            } catch (InterruptedException e) {
                System.err.println("main przerwany w czasie oczekiwania na " + cw);
            }
        }
        // Sumujemy wynik cząstkowe
        s = 0;
        for (ParSum cw : w) {
            s += cw.getResult();
        }
        System.out.println(s / n);
    }
}
