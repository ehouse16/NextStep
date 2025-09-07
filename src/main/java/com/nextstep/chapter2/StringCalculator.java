package com.nextstep.chapter2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
