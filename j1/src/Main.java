import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Main server <issuerName> <port> <threadCount>");
            System.out.println(
                    "Or: java Main client <serverHost> <serverPort> <name> [delaySeconds] [exitBeforeReading]");
            return;
        }

        if ("server".equals(args[0])) {
            String issuerName = args[1];
            int port = args.length > 2 ? Integer.parseInt(args[2]) : 8000;
            int threadCount = args.length > 3 ? Integer.parseInt(args[3]) : Runtime.getRuntime().availableProcessors();

            try {
                Server server = new Server(issuerName, threadCount, port);
                server.run();
                System.out.println("Server started on port " + port + " with " + threadCount + " threads");

                Thread.currentThread().join();
            } catch (IOException e) {
                System.err.println("Failed to start server: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Server interrupted");
            }

        } else if ("client".equals(args[0])) {
            String[] clientArgs = new String[args.length - 1];
            System.arraycopy(args, 1, clientArgs, 0, clientArgs.length);
            Client.main(clientArgs);
        }
    }
}