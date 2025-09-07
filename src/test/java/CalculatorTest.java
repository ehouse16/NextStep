import com.nextstep.chapter2.Calculator;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorTest {
    private Calculator calculator;

    @BeforeEach
    public void setUp(){
        calculator = new Calculator();
        System.out.println("Before each test");
    }

    @Test
    public void add(){
        assertEquals(9, calculator.add(6,3));
        System.out.println("Add");
    }

    @Test
    public void subtract(){
        assertEquals(3, calculator.subtract(6,3));
        System.out.println("Subtract");
    }

    @AfterEach
    public void tearDown(){
        System.out.println("After each test");
    }
}
