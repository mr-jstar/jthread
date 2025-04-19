package parIterative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import static parIterative.IterativeSolversImplementation.gaussSeidel;
import static parIterative.IterativeSolversImplementation.jacobi;
import static parIterative.IterativeSolversImplementation.parallel_GaussSeidel_SOR;
import static parIterative.IterativeSolversImplementation.parallel_jacobiB;

/**
 *
 * @author jstar
 */

/* Test for parallel sparse solvers: jacobi and Gauss-Seidel */
public class EffectiveIterativeSolvers {

    public static void checkX(double[] x) {
        int n = x.length;
        if (n < 20) {
            for (int r = 0; r < n; r++) {
                System.out.println(x[r]);
            }
        }

        for (int r = 0; r < n; r++) {
            if (Math.abs(x[r] - r) > 1e-3) {
                System.out.println("x[" + r + "]=" + x[r] + " mi się nie podoba!");
            }
        }

    }

    public static void main(String[] args) {
        double a0 = args.length > 0 ? Double.parseDouble(args[0]) : 1000;
        int n = (int) a0;
        int sp = (int) (n * (a0 - n)) == 0 ? n / 10 : (int) (n * (a0 - n));
        double omega = args.length > 1 ? Double.parseDouble(args[1]) : 1.0;

        int cores = Runtime.getRuntime().availableProcessors();
        ArrayList<Integer> p = new ArrayList<>();
        if (args.length > 2) {
            try {
                for (int i = 2; i < args.length; i++) {
                    p.add(Integer.valueOf(args[i]));
                }
            } catch (NumberFormatException e) {
            }
        } else {
            p.add(cores / 2);
            p.add(cores);
        }

        System.out.println("Buduję rzadki układ " + n + " równań z około " + n * sp + " niezerowymi współczynnikami.");
        SparseMatrix mHA = new HashMapSparseMatrix(n);
        Random rnd = new Random();
        for (int row = 0; row < n; row++) {
            for (int t = 0; t < sp; t++) {
                int col = rnd.nextInt(n);
                mHA.set(row, col, rnd.nextDouble());
            }
            mHA.set(row, row, (1.0 + rnd.nextDouble()) * sp);
        }
        SparseMatrix mA = null;
        if (args.length > 0 && args[args.length - 1].equals("-nocrs")) {
            mA = mHA;
        } else {
            System.out.println("Konwertuję macierz do formatu CRS.");
            mA = ((HashMapSparseMatrix) mHA).toCRS();
        }
        double[] cols = new double[n];
        for (int c = 0; c < n; c++) {
            cols[c] = c;
        }
        double [] b = mA.multiply(cols, 0, n);
        double[] x0 = new double[n];
        Arrays.fill(x0, 0.0);
        double[] x = new double[n];
        Arrays.fill(x, 0.0);
        System.out.println("Rozwiązuję:");
        System.out.flush();

        long start = System.nanoTime();
        int it = jacobi(mA, b, x0, n, 1e-9, x);
        long end = System.nanoTime();
        long duration = end - start;
        System.out.println("Metoda                                L.w.   #it     Czas [ms]      S       ε");
        System.out.println("-------------------------------------------------------------------------------");
        final String FMT = "%-35.35s    %2d    %3d    %10.3f   %5.2f   %5.2f";
        System.out.println(String.format(FMT, "Sekwencyjnie Jacobi", 1, it, duration / 1e6, 1.0, 1.0));
        double S1j = duration;
        checkX(x);

        for (int nThreads : p) {
            Arrays.fill(x0, 0.0);
            Arrays.fill(x, 0.0);
            start = System.nanoTime();
            it = parallel_jacobiB(mA, b, x0, n, 1e-9, x, nThreads);
            end = System.nanoTime();
            duration = end - start;
            System.out.println(String.format(FMT, "Współbieżnie Jacobi", nThreads, it, duration / 1e6, S1j / duration, ((duration / S1j - 1 / nThreads) / (1 - 1 / nThreads))));
            checkX(x);
        }
        System.out.println("-------------------------------------------------------------------------------");

        Arrays.fill(x0, 0.0);
        Arrays.fill(x, 0.0);
        start = System.nanoTime();
        it = gaussSeidel(mA, b, x0, n, 1e-9, x);
        end = System.nanoTime();
        duration = end - start;
        System.out.println(String.format(FMT, "Sekwencyjnie Gauss-Seidel", 1, it, duration / 1e6, 1.0, 1.0));
        double S1gs = duration;
        checkX(x);

        for (int nThreads : p) {
            Arrays.fill(x0, 0.0);
            Arrays.fill(x, 0.0);
            start = System.nanoTime();
            it = parallel_GaussSeidel_SOR(mA, b, x0, n, omega, 1e-9, x, nThreads);
            end = System.nanoTime();
            duration = end - start;
            System.out.println(String.format(FMT, "Współbieżnie Gauss-Seidel " + (omega < 1 ? "(SOR)" : "     "), nThreads, it, duration / 1e6, S1gs / duration, ((duration / S1gs - 1 / nThreads) / (1 - 1 / nThreads))));
            checkX(x);
        }
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("Koniec.");

        if (args.length == 0) {
            System.out.println("""
                               \n\n
                               Przy uruchomieniu klasy możesz podać argumenty, które oznaczają kolejno:
                               \t - <wielkość układu równań>.<wypełnienie>
                               \t - współczynnik nadrelaksacji (omega) dla SOR:  1 -- bez SOR, < 1-- z S)R
                               \t - listę liczb wątków, które mają być uruchomione w algorytmach równoległych
                               \t - opcjonalnie klucz -nocrs wyłączający użycie Compressed Row Storage
                               
                               Np.
                               java IterativeSolvers  5000.01 1 2 4 6 8 10 12 
                                uruchomi solvery dla układu 5000 równań o wypełnieniu 1% (~50 niezerowych współczynników w wierszu),
                                bez SOR, z użyciem kolejno 2,4,6..12 wątków.
                               
                               Progam wypisuje czasy rozwiązań i wyznacza przyspieszenie wynikające ze zrównoleglenia (S(1)/S(p)) oraz
                               wartość metryki Karpa-Flatta (oszacowanie części sekwencyjnej = (1/S-1/p)/(1-1/p) )
                               """);
        }
    }
}
