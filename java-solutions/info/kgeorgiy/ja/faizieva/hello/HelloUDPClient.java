package info.kgeorgiy.ja.faizieva.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threads);
        SocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (UnknownHostException e) {
            System.err.println("Incorrect host : " + host);
            return;
        }

        for (int j = 1; j <= threads; j++) {
            final int thread = j;
            threadPool.submit(() -> start(socketAddress, prefix, thread, requests));
        }

        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            System.err.println("Enable to terminate threads" + e.getMessage());
        }
    }

    private void start(SocketAddress socketAddress, String prefix, int thread, int requests) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(100);
            int size = socket.getReceiveBufferSize();
            for (int i = 1; i <= requests; i++) {
                boolean flag = false;
                while (!flag && !socket.isClosed() && !Thread.interrupted()) {
                    try {
                        String request = prefix + thread + "_" + i;
                        byte[] requestBytes = request.getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(requestBytes, requestBytes.length, socketAddress));

                        DatagramPacket packet = new DatagramPacket(new byte[size], size);
                        socket.receive(packet);
                        String response = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

                        if ((response.contains(request + " ") || response.endsWith(request))) {
                            flag = true;
                            System.out.println("Request is " + request);
                            System.out.println("Response is " + response);
                        }
                    } catch (IOException e) {
                        System.err.println("Unable to send request or receive response" + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Can't connect to address " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Incorrect arguments");
            return;
        }
        int port, threads, requests;
        try {
            port = Integer.parseInt(args[1]);
            threads = Integer.parseInt(args[3]);
            requests = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Incorrect arguments" + e.getMessage());
            return;
        }
        new HelloUDPClient().run(args[0], port, args[2], threads, requests);
    }
}
