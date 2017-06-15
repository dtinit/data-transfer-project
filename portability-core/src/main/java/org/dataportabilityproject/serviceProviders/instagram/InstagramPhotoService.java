package org.dataportabilityproject.serviceProviders.instagram;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.serviceProviders.instagram.model.MediaFeedData;
import org.dataportabilityproject.serviceProviders.instagram.model.MediaResponse;

final class InstagramPhotoService implements Exporter<PhotosModelWrapper> {
  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final String FAKE_ALBUM_ID = "instagramAlbum";

  private final String token;
  private final HttpTransport httpTransport;

  InstagramPhotoService(String token) {
    try {
      this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      this.token = token;
    } catch (IOException | GeneralSecurityException e) {
      throw new IllegalStateException("Problem getting token", e);
    }
  }

  @Override
  public PhotosModelWrapper export(ExportInformation exportInformation) throws IOException {
    MediaResponse response = makeRequest(
        "https://api.instagram.com/v1/users/self/media/recent",
        MediaResponse.class);

    List<PhotoModel> photos = new ArrayList<>();

    // TODO: check out paging.
    for (MediaFeedData photo : response.getData()) {
      // TODO json mapping is broken.
      photos.add(new PhotoModel("Instagram photo: " + photo.getId(),
          photo.getImages().getStandardResolution().getUrl(),
          photo.getCaption().getText(),
          null,
          FAKE_ALBUM_ID));
    }

    List<PhotoAlbum> albums = new ArrayList<>();

    if (!photos.isEmpty() && !exportInformation.getPaginationInformation().isPresent()) {
      albums.add(new PhotoAlbum(
          FAKE_ALBUM_ID,
          "Imported Instagram Photos",
          "Photos imported from instagram"));
    }

    return new PhotosModelWrapper(albums, photos, null);
  }

  private <T> T makeRequest(String url, Class<T> clazz) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    HttpRequest getRequest = requestFactory.buildGetRequest(
        new GenericUrl(url + "?access_token=" + token));
    HttpResponse response = getRequest
        .execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException("Bad status code: " + statusCode + " error: "
          + response.getStatusMessage());
    }
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent(), Charsets.UTF_8));
    return MAPPER.readValue(result, clazz);
  }
}
