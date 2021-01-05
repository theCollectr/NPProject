import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;

public class QuestionToJson {
    public static void main(String[] args) {
        ArrayList<Question> questions = readQuestions();
        JSONArray jsonArray = new JSONArray();
        for (Question question : questions) {
            jsonArray.put(questionToJson(question));
        }
        writeQuestions(jsonArray);
    }

    static ArrayList<Question> readQuestions() {
        ArrayList<Question> ret = new ArrayList<>();
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream("resources/questions.out"))) {
            ret = (ArrayList<Question>) stream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    static JSONObject questionToJson(Question question) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("question", question.getQuestion());
        jsonObject.put("choices", question.getChoices());
        jsonObject.put("points", question.getPoints());
        jsonObject.put("correct", question.getCorrectAnswer());
        return jsonObject;
    }

    static void writeQuestions(JSONArray questions) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("resources/questions.json"))) {
            writer.write(questions.toString(4));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}