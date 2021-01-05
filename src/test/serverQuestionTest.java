package test;

import server.QuestionsGenerator;

import java.util.Arrays;

public class serverQuestionTest {
    public static void main(String[] args) {
        QuestionsGenerator generator = new QuestionsGenerator();
        System.out.println(Arrays.toString(generator.getRandomQuestionsSet(10)));
    }
}
