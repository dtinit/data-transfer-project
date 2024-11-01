import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GooglePhotosInterfaceTest {

    private GooglePhotosInterface googlePhotosInterface;
    private HttpRequestFactory requestFactory;
    private HttpRequest httpRequest;
    private HttpResponse httpResponse;

    @BeforeEach
    public void setUp() throws Exception {
        // Mock dependencies
        requestFactory = mock(HttpRequestFactory.class);
        httpRequest = mock(HttpRequest.class);
        httpResponse = mock(HttpResponse.class);

        // Initialize GooglePhotosInterface with mocked dependencies
        googlePhotosInterface = new GooglePhotosInterface(/* parameters */);
        when(requestFactory.buildGetRequest(any(GenericUrl.class))).thenReturn(httpRequest);
    }

    @Test
    public void testListAlbums() throws Exception {
        // Prepare mock response
        String jsonResponse = "{\"albums\": [{\"id\": \"1\", \"title\": \"Vacation\"}]}";
        when(httpRequest.execute()).thenReturn(httpResponse);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContent()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes()));

        // Call the method under test
        AlbumListResponse response = googlePhotosInterface.listAlbums(Optional.empty());

        // Verify results
        assertNotNull(response);
        assertEquals(1, response.getAlbums().size());
        assertEquals("1", response.getAlbums().get(0).getId());
        assertEquals("Vacation", response.getAlbums().get(0).getTitle());
    }
}
