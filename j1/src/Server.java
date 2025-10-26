import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
// import java.security.PublicKey;
import java.util.Date;
import java.util.concurrent.*;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class Server {
    record GenerationResult(KeyPair keyPair, X509CertificateHolder certificate) {
    }

    private final String issuerName;
    private ServerSocket serverSocket;
    private Thread.Builder threadBuilder;
    private final ConcurrentHashMap<String, Future<GenerationResult>> storage = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor generatingThreadPool;
    private final PrivateKey caPrivateKey;

    public Server(String issuerName, int coreThreadsCount, int port) throws IOException {
        this.issuerName = issuerName;
        serverSocket = new ServerSocket(port);
        threadBuilder = Thread.ofVirtual();
        generatingThreadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(coreThreadsCount);

        try (PEMParser pemParser = new PEMParser(new FileReader("secret/server_private_key.pem"))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            caPrivateKey = converter.getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) object);
        }
    }

    public void run() throws IOException {
        threadBuilder.start(this::acceptConnections);
    }

    private void acceptConnections() {
        while (true) {
            try {
                Socket newConnSocket = serverSocket.accept();
                threadBuilder.start(() -> processConnection(newConnSocket));
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private void processConnection(Socket connSocket) {
        try (connSocket;
                InputStream inputStream = connSocket.getInputStream();
                OutputStream outputStream = connSocket.getOutputStream()) {

            String name = handleName(inputStream);
            if (name == null || name.isEmpty()) {
                return;
            }

            Future<GenerationResult> future = storage.computeIfAbsent(name,
                    k -> generatingThreadPool.submit(() -> generateKeyPairAndCertificate(k)));

            GenerationResult result = future.get();
            sendKeyPairAndCertificate(outputStream, result);

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private GenerationResult generateKeyPairAndCertificate(String subjectName) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(8192);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            Date notBefore = new Date();
            Date notAfter = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);

            X500Name issuer = new X500Name("CN=" + issuerName);
            X500Name subject = new X500Name("CN=" + subjectName);

            SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
                    keyPair.getPublic().getEncoded());

            X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                    issuer,
                    BigInteger.valueOf(System.currentTimeMillis()),
                    notBefore,
                    notAfter,
                    subject,
                    publicKeyInfo);

            JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
            ContentSigner contentSigner = signerBuilder.build(caPrivateKey);

            X509CertificateHolder certificate = certificateBuilder.build(contentSigner);

            return new GenerationResult(keyPair, certificate);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String handleName(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] readBuffer = new byte[100];

        while (true) {
            int bytesRead = inputStream.read(readBuffer);
            if (bytesRead == -1) {
                break;
            }

            boolean foundNull = false;
            for (int i = 0; i < bytesRead; i++) {
                if (readBuffer[i] == 0) {
                    buffer.write(readBuffer, 0, i);
                    foundNull = true;
                    break;
                }
            }

            if (!foundNull) {
                buffer.write(readBuffer, 0, bytesRead);
            } else {
                break;
            }
        }

        return buffer.toString(StandardCharsets.US_ASCII.name());
    }

    private void sendKeyPairAndCertificate(OutputStream outputStream, GenerationResult result) throws IOException {
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new OutputStreamWriter(outputStream))) {
            pemWriter.writeObject(result.keyPair().getPrivate());
            pemWriter.writeObject(result.keyPair().getPublic());
            pemWriter.writeObject(result.certificate());
        }
    }

    public void stop() throws IOException {
        generatingThreadPool.shutdown();
        serverSocket.close();
    }
}