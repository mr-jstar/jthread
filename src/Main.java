/**
 *
 * @author jstar
 */
public class Main {
    public static void main( String [] args ){
        System.out.println("""
                           Menu: 
                                parReduction - równoległa redukcja wektora
                                    ParRed - uruchamia wątki, czeka aż skończą, pobiera i sumuje wyniki cząstkowe
                                    ParRedQ - wykorzystuje synchronizowaną kolejkę do zbierania wyników cząstkowych
                                
                                parJacobi - równoległy algorytm iteracji prostych Jacobiego
                                    JacobiSolver - buduje i rozwiązuje rzadki, zdominowany układ równań o zadanej wielkości
                                                   porównuje rozwiązanie sekwencyjne z rozwiązaniem na tylu wątkach ile jest rdzeni
                                                   i z rozwiązaniem na połowie liczby rdzeni.
                           """);
    }

}
