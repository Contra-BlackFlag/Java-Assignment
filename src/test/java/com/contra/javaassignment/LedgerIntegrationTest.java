package com.contra.javaassignment;

import com.contra.javaassignment.model.Wallet;
import com.contra.javaassignment.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LedgerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WalletRepository walletRepository;

    private final HttpClient client = HttpClient.newHttpClient();

    private String getEndpointUrl() {
        return "http://localhost:" + port + "/api/v1/transactions/process";
    }

    @BeforeEach
    void cleanDatabase() {
        walletRepository.deleteAll();
    }

    @Test
    @DisplayName("Processes a single valid debit transaction successfully.")
    void testHappyPath() throws Exception {
        // Step 1: Create a initial wallet with 500 balance
        UUID userId = UUID.randomUUID();
        Wallet wallet = new Wallet(userId, new BigDecimal("500.00"));
        walletRepository.save(wallet);

        UUID transactionId = UUID.randomUUID();
        String jsonBody = "{"
                + "\"transactionId\": \"" + transactionId + "\","
                + "\"userId\": \"" + userId + "\","
                + "\"amount\": 100.00,"
                + "\"type\": \"DEBIT\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getEndpointUrl()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        Wallet updatedWallet = walletRepository.findById(userId).get();
        assertEquals(0, new BigDecimal("400.00").compareTo(updatedWallet.getBalance()));

        System.out.println("Result: Happy Path Test Passed. Balance: " + updatedWallet.getBalance());
    }

    @Test
    @DisplayName("Sends 3 identical transactionIDs simultaneously. Ensures the balance is only deducted once.")
    void testIdempotency() throws Exception {
        // Step 1: Prepare test data with one shared transactionId
        UUID userId = UUID.randomUUID();
        UUID sameTransactionId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        String payload = "{"
                + "\"transactionId\": \"" + sameTransactionId + "\","
                + "\"userId\": \"" + userId + "\","
                + "\"amount\": 100.00,"
                + "\"type\": \"DEBIT\""
                + "}";

        List<Integer> responseCodes = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Thread t = new Thread(() -> {
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(getEndpointUrl()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    responseCodes.add(res.statusCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        int successCount = 0;
        int conflictCount = 0;

        for (int code : responseCodes) {
            if (code == 200) {
                successCount++;
            } else if (code == 409) {
                conflictCount++;
            }
        }

        assertEquals(1, successCount, "Only 1 transaction should succeed");
        assertEquals(2, conflictCount, "Duplicate requests should return 409 Conflict");

        Wallet finalWallet = walletRepository.findById(userId).get();
        assertEquals(0, new BigDecimal("400.00").compareTo(finalWallet.getBalance()));

        System.out.println("Result: Idempotency Test Passed. OK: " + successCount + ", Conflict: " + conflictCount);
    }

    @Test
    @DisplayName("Sends 10 concurrent debit requests of 100 for a wallet with a 500 balance. Ensures the final balance is exactly 0 and 5 requests fail with insufficient funds.")
    void testRaceCondition() throws Exception {
        UUID userId = UUID.randomUUID();
        walletRepository.save(new Wallet(userId, new BigDecimal("500.00")));

        List<Integer> responseCodes = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
                try {
                    String payload = "{"
                            + "\"transactionId\": \"" + UUID.randomUUID() + "\","
                            + "\"userId\": \"" + userId + "\","
                            + "\"amount\": 100.00,"
                            + "\"type\": \"DEBIT\""
                            + "}";

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(getEndpointUrl()))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    responseCodes.add(res.statusCode());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        int successCount = 0;
        int failedCount = 0;

        for (int code : responseCodes) {
            if (code == 200) {
                successCount++;
            } else if (code == 400) {
                failedCount++;
            }
        }

        assertEquals(5, successCount, "Exactly 5 debits should succeed");
        assertEquals(5, failedCount, "Exactly 5 debits should fail due to lack of funds");

        Wallet finalWallet = walletRepository.findById(userId).get();
        assertEquals(0, BigDecimal.ZERO.compareTo(finalWallet.getBalance()), "Final balance must be exactly 0");

        System.out.println("Result: Race Condition Test Passed. Successes: " + successCount + ", Failures: " + failedCount);
    }
}