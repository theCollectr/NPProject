package test;

import java.util.Timer;
import java.util.TimerTask;

public class TimerTest {
    public static void main(String[] args) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("task1");
            }
        };
        timer.schedule(task, 1000);
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                System.out.println("task2");
//            }
//        }, 1000);
    }
}
