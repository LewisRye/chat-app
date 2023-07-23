import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * THE CLI IS FOR DEBUGGING, DO NOT USE THIS FOR ANY OTHER PURPOSE.
 */

public class CLI implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean finished;

    public CLI() {
        finished = false;
    }

    @Override
    public void run() {
        try {
            client = new Socket("localhost", 65000);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            System.out.println("CLI version of ChatApp, use this for debugging ONLY.");
            System.out.println("Enter a unique nickname consisting of 3 to 12 characters: ");

            InputHandler handler = new InputHandler();
            Thread thread = new Thread(handler);
            thread.start();

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void shutdown() {
        finished = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()) {
                client.close();
            }
        } catch (Exception e) {
            System.err.println("Could not establish a connection with the server. " +
                    "If you are running this server locally, try running the 'Server.java' file.");
        }
    }

    class InputHandler implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                while (!finished) {
                    String message = inputReader.readLine();
                    if (message.equals("/quit")) {
                        out.println(message);
                        shutdown();
                        inputReader.close();
                    } else {
                        out.println(message);
                    }
                }
            } catch (Exception e) { }
        }
    }

    public static void main(String[] args) {
        CLI client = new CLI();
        client.run();
    }
}
