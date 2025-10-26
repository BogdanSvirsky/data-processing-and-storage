import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

public class Client {
    private final String serverHost;
    private final int serverPort;
    private final String name;
    private final int delaySeconds;
    private final boolean exitBeforeReading;

    public Client(String serverHost, int serverPort, String name, int delaySeconds, boolean exitBeforeReading) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.name = name;
        this.delaySeconds = delaySeconds;
        this.exitBeforeReading = exitBeforeReading;
    }

    public void run() {
        try (Socket socket = new Socket(serverHost, serverPort);
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream()) {

            byte[] nameBytes = name.getBytes(StandardCharsets.US_ASCII);
            outputStream.write(nameBytes);
            outputStream.write(0);
            outputStream.flush();

            System.out.println("Client name \"" + name + "\" sended");

            if (delaySeconds > 0) {
                Thread.sleep(delaySeconds * 1000L);
            }

            if (exitBeforeReading) {
                System.out.println("Exiting before reading response (simulating crash)");
                return;
            }

            saveKeyPairAndCertificate(inputStream);

        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private void saveKeyPairAndCertificate(InputStream inputStream) throws IOException {
        try (PEMParser pemParser = new PEMParser(new InputStreamReader(inputStream))) {
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            PrivateKey privateKey = converter
                    .getPrivateKey((org.bouncycastle.asn1.pkcs.PrivateKeyInfo) keyPair.getPrivateKeyInfo());
    
            PublicKey publicKey = converter.getPublicKey((SubjectPublicKeyInfo) pemParser.readObject());

            Object certificateObj = pemParser.readObject();
            X509CertificateHolder certificate = (X509CertificateHolder) certificateObj;

            try (JcaPEMWriter keyWriter = new JcaPEMWriter(new FileWriter(name + ".key"))) {
                keyWriter.writeObject(privateKey);
            }
            try (JcaPEMWriter keyWriter = new JcaPEMWriter(new FileWriter(name + "_public.key"))) {
                keyWriter.writeObject(publicKey);
            }

            try (JcaPEMWriter certWriter = new JcaPEMWriter(new FileWriter(name + ".crt"))) {
                certWriter.writeObject(certificate);
            }

            System.out.println("Successfully saved private key and certificate for: " + name);
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out
                    .println("Usage: java Client <serverHost> <serverPort> <name> [delaySeconds] [exitBeforeReading]");
            System.out.println("Example: java Client localhost 8000 alice 5 false");
            return;
        }

        String serverHost = args[0];
        int serverPort = Integer.parseInt(args[1]);
        String name = args[2];
        int delaySeconds = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        boolean exitBeforeReading = args.length > 4 && Boolean.parseBoolean(args[4]);

        Client client = new Client(serverHost, serverPort, name, delaySeconds, exitBeforeReading);
        client.run();
    }
}