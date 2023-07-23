import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean finished;
    private ExecutorService threadPool;

    public Server() {
        connections = new ArrayList<>();
        finished = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(65000);
            threadPool = Executors.newCachedThreadPool();
            while (!finished) { // constantly allows new connections
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler); // this would definitely go down if somebody spam joined
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }

    public void broadcast(String message) {
        ArrayList<String> users = new ArrayList<>();

        for (ConnectionHandler ch : connections) {
            users.add(ch.getNickname());
        }

        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
                ch.sendUserList(users);
            }
        }
    }

    public void shutdown() {
        try {
            finished = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }

    class ConnectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                // SETTING USERNAME

                boolean valid = false;
                do {
                    ArrayList<String> listOfNicknames = new ArrayList<>();
                    for (ConnectionHandler ch : connections) {
                        listOfNicknames.add(ch.getNickname());
                    }
                    nickname = in.readLine();
                    if (nickname.startsWith("/") || nickname.length() < 3 || nickname.length() > 12 || listOfNicknames.contains(nickname)) {}
                    else {
                        valid = true;
                    }
                } while (!valid);

                ZonedDateTime joinTime = ZonedDateTime.now(Clock.systemUTC());

                LocalDateTime userJoinTime = joinTime.toLocalDateTime();
                DateTimeFormatter timeJoinFormatter = DateTimeFormatter.ofPattern("dd/MM/yy, HH:mm");
                String zonedJoinTime = userJoinTime.format(timeJoinFormatter);

                System.out.println("(" + joinTime + ") " + nickname + " joined the chat."); // this is for the server log
                broadcast("(" + zonedJoinTime + ") " + nickname + " joined the chat.");

                // QUITTING THE SERVER

                String message;
                while ((message = in.readLine()) != null) {
                    ZonedDateTime time = ZonedDateTime.now(Clock.systemUTC());

                    LocalDateTime userTime = time.toLocalDateTime();
                    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy, HH:mm");
                    String zonedTime = userTime.format(timeFormatter);

                    if (message.startsWith("/quit")) {
                        System.out.println("(" + time + ") " + nickname + " left the chat."); // this is for the server log
                        broadcast("(" + zonedTime + ") " + nickname + " left the chat.");
                        for (Iterator<ConnectionHandler> it = connections.iterator(); it.hasNext();) { // removing users from the user list once they quit
                            ConnectionHandler ch = it.next();
                            if (ch.getNickname().equals(nickname)) {
                                it.remove();
                                broadcast("");
                            }
                        }
                        shutdown();
                    }
                    else {
                        broadcast("(" + zonedTime + ") " + nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void sendUserList(ArrayList<String> users) {
            String formattedUserList = "/userlist";

            for (String user : users) {
                formattedUserList = formattedUserList + (", " + user);
            }

            out.println(formattedUserList);
        }

        public String getNickname() {
            return nickname;
        }

        public void shutdown() {
            try {
                in.close();
                out.close();

                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
