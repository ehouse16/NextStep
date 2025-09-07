책은 Junit4지만 이 프로젝트는 JUnit5 사용!

```java
import com.nextstep.chapter2.Calculator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorTest {
    private Calculator calculator = new Calculator();

    @Test
    public void add(){
        assertEquals(9, calculator.add(6,3));
    }

    @Test
    public void subtract(){
        assertEquals(3, calculator.subtract(6,3));
    }
}

```

JUnit은 Calculator 같은 객체 초기화 작업을 위와 같이 구현하는 것을 추천 안함

대신 `@Before` 어노테이션 사용을 권장 
(Junit5에서는 `@BeforeEach`로 바뀜)

`Calculator`인스턴스를 매 테스트마다 생성하는 이유는 다음 테스트 실행 시 영향을 미칠 수 있기 때문. 그럼 왜 `@Before` 어노테이션을 사용해야하나?

- JUnit에는 `@RunWith`, `@Rule` 어노테이션을 사용해 기능을 확장가능한데, `@Before` 안이어야만 객체접근이 허용된다(JUnit5에서는 `@ExtendWith`, Rule 개념은 `Extension`으로 대체)

```java
public class StringCalculator {
    public int add(String text){
        // ✅ 빈 문자열 또는 null을 입력할 경우 0을 반환
        if(text == null || text.isEmpty()){
            return 0;
        }

        return sum(toInts(split(text)));
    }

    private int[] toInts(String[] words) {
        int[] numbers = new int[words.length];
        for(int i = 0; i < words.length; i++){
            numbers[i] = toPositive(words[i]);
        }

        return numbers;
    }

    // ✅ 숫자 하나를 문자열로 입력할 경우 해당 숫자를 반환
    // ✅ 음수 입력 시 RuntimeException 예외 처리
    private int toPositive(String word) {
        int number = Integer.parseInt(word);
        if(number < 0)
            throw new RuntimeException();
        return number;
    }

    private int sum(int[] words){
        int sum = 0;
        for(int number : words){
            sum += number;
        }

        return sum;
    }

    // ✅ 숫자 두 개를 쉼표 구분자로 입력할 경우 두 숫자의 합을 반환
    // ✅ 구분자를 쉼표 이외에 콜론을 사용할 수 있다
    // ✅ "//" 와 "\n" 문자 사이에 커스텀 구분자를 지정할 수 있다
    private String[] split(String words){
        Matcher m = Pattern.compile("//(.)\n(.*)").matcher(words);

        if(m.find()){
            String customDelimiter = m.group(1);
            return m.group(2).split(customDelimiter);
        }
        return words.split(",|:");
    }
}

```

구현 → 테스트를 통환 결과 확인 → 리팩토링

메서드는 한가지 책임만 가지기

리팩토링 한 후 주의 깊게 봐야할 부분

- `public` 으로 공개하고 있는 `add()`메서드가 얼마나 읽기 쉽고, 좋은가 확인
- **세부 구현은 모두 `private` 메서드로 분리해 관심사 분리**
    - 메서드 분리해두면 새로운 요구사항 발생 시 그 메서드만 고치면 됨

---

https://regexr.com/

정규식 연습 사이트

### TDD 정의

- 프로그램을 작성하기 전 테스트를 먼저 작성하는 것
- 코드를 검증하는 테스트 코드를 먼저 만든 다음에 실제 작성해야 하는 프로그램 코드 작성에 들어가기

### TDD 목표

- 잘 동작하는 깔끔한 코드
- 정상적으로 동작하는 코드 + 작성된 코드도 명확한 의미를 전달할 수 있게 작성

### 개발에 있어 TDD의 위치

- TDD에서 말하는 단위 테스트는 메서드 단위의 테스트
- 메서드나 함수 단위로 테스트하여 통합테스트나 인수 테스트에서의 결함발생 비용을 줄여준다

### TDD 진행방식
- **질문(Ask)**
    - 테스트 작성을 통해 시스템에 질문
    - = 작성하고자 하는 메서드나 기능이 무엇인지 선별하고 작성 완료 조건을 정해서 실패하는 테스트 케이스를 작성하는 것
- **응답(Respond)**
    - 테스트를 통과하는 코드를 작성해 질문에 대답
- **정제(Refine)**
    - 아이디어를 통합하고, 불필요한 것은 제거하고, 모호한 것은 명확히 해서 대답을 정제 (리팩토링)
        - 소스의 가독성이 적절한가?
        - 중복된 코드는 없는가?
        - 이름이 잘못 부여된 메서드나 변수명은 없는가?
        - 구조의 개선이 필요한 부분은 없는가?

- **반복(Repeat)**
    - 다음 질문을 통해 대화를 계속 진행