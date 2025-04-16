package parJacobi;

import parIterative.SparseMatrix;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jstar
 */
public class SparseMatrixTest {

    public SparseMatrixTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of set method, of class SparseMatrix.
     */
    @Test
    public void testSet() {
        System.out.println("set");
        double v = 7.0;
        SparseMatrix instance = new SparseMatrix(10);
        for (int i = 0; i < 10; i += 2) {
            instance.set(i, i, v);
        }
        for (int i = 1; i < 10; i += 2) {
            assertEquals(0.0, instance.get(i, i), 0.0);
        }
        for (int i = 0; i < 10; i += 2) {
            assertEquals(7.0, instance.get(i, i), 0.0);
        }
    }

    /**
     * Test of get method, of class SparseMatrix.
     */
    @Test
    public void testGet() {
        System.out.println("get");
        SparseMatrix instance = new SparseMatrix(10);
        for (int i = 0; i < 10; i++) {
            instance.set(i, 9 - i, i * i);
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(i * i, instance.get(i, 9 - i), 0.0);
        }
    }

    /**
     * Test of size method, of class SparseMatrix.
     */
    @Test
    public void testSize() {
        System.out.println("size");
        SparseMatrix instance = new SparseMatrix(1313);
        assertEquals(1313, instance.size());
    }

}
