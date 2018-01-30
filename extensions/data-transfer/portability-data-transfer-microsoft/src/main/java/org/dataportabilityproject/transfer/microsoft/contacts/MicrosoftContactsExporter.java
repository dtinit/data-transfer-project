package org.dataportabilityproject.transfer.microsoft.contacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.VCard;
import ezvcard.io.json.JCardWriter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformResult;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.types.GraphPagination;
import org.dataportabilityproject.types.transfer.auth.TokenAuthData;
import org.dataportabilityproject.types.transfer.models.contacts.ContactsModelWrapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * Exports Microsoft contacts using the Graph API.
 */
public class MicrosoftContactsExporter implements Exporter<TokenAuthData, ContactsModelWrapper> {
    private static final String CONTACTS_URL = "https://graph.microsoft.com/v1.0/me/contacts";
    private static final String ODATA_NEXT = "@odata.nextLink";

    private OkHttpClient client;
    private ObjectMapper objectMapper;
    private TransformerService transformerService;

    public MicrosoftContactsExporter(OkHttpClient client, ObjectMapper objectMapper, TransformerService transformerService) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.transformerService = transformerService;
    }

    @Override
    public ExportResult<ContactsModelWrapper> export(TokenAuthData authData) {
        return doExport(authData, CONTACTS_URL);
    }

    @Override
    public ExportResult<ContactsModelWrapper> export(TokenAuthData authData, ContinuationData continuationData) {
        GraphPagination graphPagination = (GraphPagination) continuationData.getPaginationData();
        return doExport(authData, graphPagination.getNextLink());
    }

    @SuppressWarnings("unchecked")
    private ExportResult<ContactsModelWrapper> doExport(TokenAuthData authData, String url) {
        Request.Builder graphReqBuilder = new Request.Builder().url(url);
        graphReqBuilder.header("Authorization", "Bearer " + authData.getToken());

        try (Response graphResponse = client.newCall(graphReqBuilder.build()).execute()) {
            ResponseBody body = graphResponse.body();
            if (body == null) {
                return new ExportResult<>(ExportResult.ResultType.ERROR, "Error retrieving contacts: response body was null");
            }
            String graphBody = new String(body.bytes());
            Map graphMap = objectMapper.reader().forType(Map.class).readValue(graphBody);

            String nextLink = (String) graphMap.get(ODATA_NEXT);
            ContinuationData continuationData = nextLink == null ? null : new ContinuationData(new GraphPagination(nextLink));

            List<Map<String, Object>> rawContacts = (List<Map<String, Object>>) graphMap.get("value");
            if (rawContacts == null) {
                return new ExportResult<>(ExportResult.ResultType.END);
            }

            ContactsModelWrapper wrapper = transform(rawContacts);
            return new ExportResult<>(ExportResult.ResultType.CONTINUE, wrapper, continuationData);
        } catch (IOException e) {
            e.printStackTrace();  // FIXME log error
            return new ExportResult<>(ExportResult.ResultType.ERROR, "Error retrieving contacts: " + e.getMessage());
        }
    }

    private ContactsModelWrapper transform(List<Map<String, Object>> rawContacts) {
        StringWriter stringWriter = new StringWriter();
        try (JCardWriter writer = new JCardWriter(stringWriter);) {
            for (Map<String, Object> rawContact : rawContacts) {
                TransformResult<VCard> result = transformerService.transform(VCard.class, rawContact);
                if (result.hasProblems()) {
                    // discard
                    // FIXME log problem
                    continue;
                }
                writer.write(result.getTransformed());
            }
        } catch (IOException e) {
            //TODO log
            e.printStackTrace();
            return new ContactsModelWrapper("");
        }
        return new ContactsModelWrapper(stringWriter.toString());

    }


}
