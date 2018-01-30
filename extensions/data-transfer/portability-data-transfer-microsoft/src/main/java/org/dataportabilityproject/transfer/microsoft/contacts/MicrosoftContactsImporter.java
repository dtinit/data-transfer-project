package org.dataportabilityproject.transfer.microsoft.contacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.VCard;
import ezvcard.io.json.JCardReader;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformResult;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.contacts.ContactsModelWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Performs a batch import of contacts using the Microsoft Graph API. For details see: https://developer.microsoft.com/en-us/graph/docs/concepts/json_batching.
 */
public class MicrosoftContactsImporter implements Importer<TokenAuthData, ContactsModelWrapper> {
    private static final String BATCH_URL = "https://graph.microsoft.com/beta/$batch";
    private static final String CONTACTS_URL = "me/contacts"; //must be relative for batch operations

    private OkHttpClient client;
    private ObjectMapper objectMapper;
    private TransformerService transformerService;

    public MicrosoftContactsImporter(OkHttpClient client, ObjectMapper objectMapper, TransformerService transformerService) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.transformerService = transformerService;
    }

    @Override
    public ImportResult importItem(TokenAuthData authData, ContactsModelWrapper wrapper) {
        JCardReader reader = new JCardReader(wrapper.getVCards());
        try {
            List<VCard> cards = reader.readAll();

            List<String> problems = new ArrayList<>();

            int[] id = new int[]{1};
            List<Map<String, Object>> requests = cards.stream().map(card -> {
                TransformResult<LinkedHashMap> result = transformerService.transform(LinkedHashMap.class, card);
                problems.addAll(result.getProblems());
                LinkedHashMap contact = result.getTransformed();
                Map<String, Object> request = createRequest(id[0], contact);
                id[0]++;
                return request;
            }).collect(toList());

            if (!problems.isEmpty()) {
                // TODO log problems
            }

            Map<String, Object> batch = new LinkedHashMap<>();
            batch.put("requests", requests);

            Request.Builder requestBuilder = new Request.Builder().url(BATCH_URL);
            requestBuilder.header("Authorization", "Bearer " + authData.getToken());
            requestBuilder.post(RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(batch)));
            try (Response response = client.newCall(requestBuilder.build()).execute()) {
                int code = response.code();
                if (code >= 200 && code <= 299) {
                    // some contacts may already exist and be returned as 200
                    return new ImportResult(ImportResult.ResultType.OK);
                } else {
                    // FIXME evaluate HTTP response and return whether to retry
                    return new ImportResult(ImportResult.ResultType.ERROR);
                }
            }
        } catch (IOException e) {
            // TODO log
            e.printStackTrace();
            return new ImportResult(ImportResult.ResultType.ERROR, "Error deserializing contacts: " + e.getMessage());
        }
    }

    private Map<String, Object> createRequest(int i, LinkedHashMap contact) {
        Map<String, Object> request = new LinkedHashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        request.put("headers", headers);
        request.put("id", i + "");
        request.put("method", "POST");
        request.put("url", CONTACTS_URL);
        request.put("body", contact);
        return request;
    }
}
