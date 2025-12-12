import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;

class Main {
    class ResponseBody {
        String message;
        String[] successors;
    }

    private static final ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();
    private static final HttpClient client = HttpClient.newBuilder()
            .executor(service)
            .build();
    private static final List<CompletableFuture<String>> futureMessages = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        service.execute(() -> process("/"));

        service.awaitTermination(200, TimeUnit.SECONDS);

        System.out.println("Sorted messages:");
        futureMessages.stream().map(CompletableFuture::join).sorted().collect(Collectors.toList())
                .forEach(System.out::println);
    }

    private static void process(String path) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080" + path))
                .GET()
                .build();
        synchronized (client) {
            try {
                futureMessages.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(HttpResponse::body)
                        .thenApply(Main::processBody));
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    };

    private static String processBody(String body) {
        Gson gson = new Gson();
        ResponseBody responseBody = gson.fromJson(body, ResponseBody.class);
        for (String successor : responseBody.successors) {
            service.execute(() -> process("/" + successor));
            System.out.println(successor + " received");
        }

        if (responseBody.message.isEmpty())
            System.out.println("MESSAGE IS EMPTY!");

        return responseBody.message;
    }
}