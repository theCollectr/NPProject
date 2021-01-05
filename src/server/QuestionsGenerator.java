package server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestionsGenerator {
    private final String QUESTIONS_PATH = "resources/questions.json";
    private final ArrayList<Question> questions;

    public QuestionsGenerator() {
        questions = loadQuestions();
    }

    private ArrayList<Question> loadQuestions() {
        ArrayList<Question> questions = new ArrayList<>();
        JSONArray questionsArray = getJson(QUESTIONS_PATH);
        for (int i = 0; i < questionsArray.length(); i++) {
            JSONObject questionJson = questionsArray.getJSONObject(i);
            Question question = Question.createFromJson(questionJson);
            questions.add(question);
        }

        return questions;
    }

    private JSONArray getJson(String path) {
        try {
            String mapString = new String(Files.readAllBytes(Paths.get(path)));
            return new JSONArray(mapString);
        } catch (IOException e) {
            Logger.log("couldn't read questions JSON from" + path);
        }
        return new JSONArray();
    }

    public synchronized Question[] getRandomQuestionsSet(int questionsCount) {
        if (questionsCount > questions.size()) {
            questionsCount = questions.size();
        }
        Collections.shuffle(questions);
        List<Question> list = questions.subList(0, questionsCount);
        return list.toArray(new Question[questionsCount]);
    }
}
