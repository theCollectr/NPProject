package server;

import org.json.JSONArray;
import org.json.JSONObject;

public class Question {
    private static int idCounter = 0;
    private final transient int id;
    private String question;
    private String[] choices;
    private int correctAnswer;
    private int points;


    public Question() {
        id = nextId();
    }

    public Question(String question, String[] choices, int correctAnswer, int points) {
        this.question = question;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.points = points;
        id = nextId();
    }

    public static Question createFromJson(JSONObject questionJson) {
        String question = questionJson.getString("question");
        int correctAnswer = questionJson.getInt("correct");
        int points = questionJson.getInt("points");

        JSONArray choicesJson = questionJson.getJSONArray("choices");
        String[] choices = new String[choicesJson.length()];
        for (int i = 0; i < choices.length; i++) {
            choices[i] = (String) choicesJson.get(i);
        }

        return new Question(question, choices, correctAnswer, points);
    }

    private static int nextId() {
        int id = idCounter;
        idCounter += 1;
        return id;
    }

    public int getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String[] getChoices() {
        return choices;
    }

    public void setChoices(String[] choices) {
        this.choices = choices;
    }

    public int getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(int correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        String s = question + " (" + points + " points)\n";
        for (String choice : choices) {
            s += choice + "\n";
        }
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id == question.id;
    }
}
