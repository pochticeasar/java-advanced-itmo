package info.kgeorgiy.ja.faizieva.hello;


import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket datagramSocket;
    private ExecutorService threadPool;

    @Override
    public void start(int port, int threads) {
        try {
            datagramSocket = new DatagramSocket(port);
        } catch (SocketException ignored) {
            System.err.println("Can't bind to port : " + port);
            return;
        }
        threadPool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            threadPool.submit(() -> start());
        }
    }

    private void start() {
        try {
            int size = datagramSocket.getReceiveBufferSize();
            while (!datagramSocket.isClosed() && !Thread.interrupted()) {
                try {
                    DatagramPacket request = new DatagramPacket(new byte[size], size);
                    datagramSocket.receive(request);
                    String responseString = "Hello, " + new String(request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
                    byte[] responseBytes = responseString.getBytes(StandardCharsets.UTF_8);
                    datagramSocket.send(new DatagramPacket(responseBytes, responseBytes.length, request.getSocketAddress()));
                } catch (IOException e) {
                    System.err.println("");
                }
            }
        } catch (IOException e) {
            System.err.println("Impossible to get size of buffer" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Incorrect number of arguments");
            return;
        }
        int port, threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
            new HelloUDPServer().start(port, threads);
        } catch (NumberFormatException e) {
            System.err.println("Non-integer arguments" + e.getMessage());
        }
    }

    @Override
    public void close() {
        //await termination
        datagramSocket.close();
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            System.err.println("Enable to terminate threads" + e.getMessage());
        }

    }
}