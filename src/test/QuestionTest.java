package test;

import server.Question;

public class QuestionTest {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException {
        Class<Question> questionClass = (Class<Question>) Class.forName("server.Question");
        System.out.println(questionClass);
    }
}
