package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {

    private static final int CLIENT_COUNT = 3;

    public static void main(String[] args) {
        System.out.println("BAT DAU DEMO DISTRIBUTED LOCK VOI APACHE ZOOKEEPER");

        CountDownLatch startSignal = new CountDownLatch(1);
        List<Thread> clientThreads = new ArrayList<>();

        for (int i = 1; i <= CLIENT_COUNT; i++) {
            String clientName = "Client-" + i;
            Thread clientThread = new Thread(
                    new WorkerClient(clientName, startSignal),
                    clientName + "-Thread"
            );
            clientThreads.add(clientThread);
            clientThread.start();
        }

        // Cho ba client bắt đầu yêu cầu khóa gần như cùng thời điểm.
        startSignal.countDown();

        for (Thread clientThread : clientThreads) {
            try {
                clientThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Main bi gian doan khi cho cac client ket thuc.");
                break;
            }
        }

        System.out.println("KET THUC DEMO");
    }
}
