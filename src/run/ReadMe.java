package run;

/**
 *
 * @author jstar
 */
public class ReadMe {
    public static void main( String [] args ){
        System.out.println("""
                           Menu: 
                                parReduction - równoległa redukcja wektora
                                    ParRed - uruchamia wątki, czeka aż skończą, pobiera i sumuje wyniki cząstkowe
                                    ParRedQ - wykorzystuje synchronizowaną kolejkę do zbierania wyników cząstkowych
                                
                                parIterative - równoległe algorytmy iteracyjne Jacobiego i Gauss-Seidel (SOR)
                                    IterativeSolvesr - buduje i rozwiązuje rzadki, zdominowany diagonalnie układ równań o zadanej wielkości
                                                   porównuje rozwiązanie sekwencyjne z rozwiązaniem domyślnie na tylu wątkach ile jest rdzeni
                                                   i z rozwiązaniem na połowie liczby rdzeni.
                                                   Opcjonalne argumenty to <liczba-równań> <omega dla SOR> <w1> ... <wn>
                                                   gdzie <w1> ... <wn> to testowane liczby wątków
                           """);
    }

}
