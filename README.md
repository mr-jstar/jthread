
     parReduction - równoległa redukcja wektora
         ParRed - uruchamia wątki, czeka aż skończą, pobiera i sumuje wyniki cząstkowe
         ParRedQ - wykorzystuje synchronizowaną kolejkę do zbierania wyników cząstkowych

     parJacobi - równoległy algorytm iteracji prostych Jacobiego
         JacobiSolver - buduje i rozwiązuje rzadki, zdominowany diagonalnie układ równań o zadanej wielkości
                        porównuje rozwiązanie sekwencyjne z rozwiązaniem na tylu wątkach ile jest rdzeni
                        i z rozwiązaniem na połowie liczby rdzeni.
                        Wersja A (JacobiPartSolverA) - każdy w watków-wykonawców estymuje lokalnie normę poprawki i decyduje, czy kończy - może to zmienić zarządca
                        Wersja B (JacobiPartSolverB) - zarządca oblicza normę poprawki i decyduje, czy wszyscy kończą
