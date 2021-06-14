package org.datatransferproject.transfer.photobucket.client;

import com.google.api.client.auth.oauth2.Credential;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.transfer.photobucket.data.PhotobucketAlbum;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;

import java.io.IOException;
import java.util.UUID;

import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.*;

public class PhotobucketClient {
  private final String bearer;
  private final OkHttpClient httpClient;
  private final TemporaryPerJobDataStore jobStore;
  private final UUID jobId;
  private String pbRootAlbumId;

  public PhotobucketClient(
          UUID jobId, Credential credential, OkHttpClient httpClient, TemporaryPerJobDataStore jobStore) {
    this.jobId = jobId;
    this.bearer = "Bearer " + credential.getAccessToken();
    this.httpClient = httpClient;
    this.jobStore = jobStore;
  }

  public void createTopLevelAlbum(String name) throws IOException {
    // in case if albumId was not found while migrating photos,
    // we will migrate them into this top level album to avoid data loss
    PhotoAlbum photoAlbum = new PhotoAlbum(jobId.toString(), name, "");
    createAlbum(photoAlbum, "");
  }

  public void createAlbum(PhotoAlbum photoAlbum, String namePrefix) throws IOException {
    // create builder for graphQL request
    Request.Builder createAlbumRequestBuilder = new Request.Builder().url(GQL_URL);
    // add authorization headers
    createAlbumRequestBuilder
        .header(AUTHORIZATION_HEADER, bearer)
        .header(CORRELATION_ID_HEADER, jobId.toString());
    // create GQL query as string value for post request
    String query = createGQLViaRestMutation(photoAlbum, namePrefix);
    FormBody.Builder bodyBuilder = new FormBody.Builder().add("query", query);
    createAlbumRequestBuilder.post(bodyBuilder.build());
    // create album
    Response createAlbumResponse = httpClient.newCall(createAlbumRequestBuilder.build()).execute();
    // gql server always provides 200 response code. If not, interrupt job
    if (createAlbumResponse.code() == 200) {
      try {
        // get photobucket albumId from response
        String pbAlbumId = extractPBAlbumId(createAlbumResponse);
        // add photobucket albumId to the internal store, to match photos with proper albums
        jobStore.create(jobId, photoAlbum.getId(), new PhotobucketAlbum(pbAlbumId));
      } catch (Exception e) {
        // do not proceed only if top level album was not created
        if (photoAlbum.getId().equals(jobId.toString())) {
          throw e;
        }
      }
    } else {
      throw new IOException(
          String.format(
              "Wrong status code=[%s] provided by GQL server for jobId=[%s]",
              createAlbumResponse.code(), jobId));
    }
  }

  public void uploadPhoto(PhotoModel photoModel) {}

  private String createGQLViaRestMutation(PhotoAlbum photoAlbum, String prefix) {
    String pbParentId = getParentPBAlbumId(photoAlbum.getId());

    return String.format(
        "mutation createAlbum {  createAlbum(title: \"%s\", parentAlbumId: \"%s\"){ id }}",
        prefix + photoAlbum.getName(), pbParentId);
  }

  private String getPBRootAlbumId() {
    return pbRootAlbumId;
  }

  private String getParentPBAlbumId(String albumId) {
    return "";
  }

  private String extractPBAlbumId(Response response) {
    return "";
  }
}
