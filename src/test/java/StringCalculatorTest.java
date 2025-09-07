import com.nextstep.chapter2.StringCalculator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringCalculatorTest {
    private StringCalculator stringCalculator;
    @BeforeEach
    public void setup(){
        stringCalculator = new StringCalculator();
    }

    @Test
    public void add_null_또는_빈문자(){
        assertEquals(0, stringCalculator.add(null));
        assertEquals(0, stringCalculator.add(""));
    }

    @Test
    public void 숫자하나_문자열(){
        assertEquals(1, stringCalculator.add("1"));
    }

    @Test
    public void add_쉼표구분(){
        assertEquals(3, stringCalculator.add("1,2"));
    }

    @Test
    public void add_쉼표구분_콜론구분(){
        assertEquals(6, stringCalculator.add("1,2:3"));
    }

    @Test
    public void add_음수(){
        Assertions.assertThrows(RuntimeException.class, () -> {
            stringCalculator.add("-1");
        });
    }

    @Test
    public void custom_구분자(){
        assertEquals(6, stringCalculator.add("//;\n1;2;3"));
    }
}
