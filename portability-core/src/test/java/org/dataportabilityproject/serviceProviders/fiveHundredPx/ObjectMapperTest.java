package org.dataportabilityproject.serviceProviders.fiveHundredPx;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import org.dataportabilityproject.serviceProviders.fiveHundredPx.model.FiveHundredPxPhoto;
import org.dataportabilityproject.serviceProviders.fiveHundredPx.model.FiveHundredPxPhotoResponse;
import org.junit.Test;

public class ObjectMapperTest {

  private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new com.fasterxml.jackson.databind.ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .enable(SerializationFeature.INDENT_OUTPUT);

  @Test
  public void testObjectMapperPhoto() throws IOException {
    String photoString = "{\"photo\":{\"id\":242773583,\"user_id\":25019057,\"name\":\"gradient2\",\"description\":null,\"camera\":null,\"lens\":null,\"focal_length\":null,\"iso\":null,\"shutter_speed\":null,\"aperture\":null,\"times_viewed\":17,\"rating\":61.1,\"status\":1,\"created_at\":\"2018-01-16T17:28:33-05:00\",\"category\":0,\"location\":null,\"latitude\":null,\"longitude\":null,\"taken_at\":null,\"hi_res_uploaded\":0,\"for_sale\":false,\"width\":640,\"height\":400,\"votes_count\":6,\"favorites_count\":0,\"comments_count\":3,\"nsfw\":false,\"sales_count\":0,\"for_sale_date\":null,\"highest_rating\":69.3,\"highest_rating_date\":\"2018-01-16T17:52:14-05:00\",\"license_type\":0,\"converted\":false,\"collections_count\":1,\"crop_version\":0,\"image_format\":\"jpeg\",\"positive_votes_count\":6,\"privacy\":false,\"profile\":true,\"for_critique\":false,\"critiques_callout_dismissed\":false,\"exclude_gads\":false,\"url\":\"/photo/242773583/gradient2-by-anna-olson\",\"image_url\":\"https://drscdn.500px.org/photo/242773583/m%3D900/v2?client_application_id=11&user_id=25019057&webp=true&sig=ea37aa1589f9f7b72484d7a77721c5c088a7e7f45ccb681c255756ca1077c9bf\",\"images\":[{\"size\":4,\"url\":\"https://drscdn.500px.org/photo/242773583/m%3D900/v2?client_application_id=11&user_id=25019057&webp=true&sig=ea37aa1589f9f7b72484d7a77721c5c088a7e7f45ccb681c255756ca1077c9bf\",\"https_url\":\"https://drscdn.500px.org/photo/242773583/m%3D900/v2?client_application_id=11&user_id=25019057&webp=true&sig=ea37aa1589f9f7b72484d7a77721c5c088a7e7f45ccb681c255756ca1077c9bf\",\"format\":\"jpeg\"}],\"store_download\":false,\"store_print\":false,\"store_license\":false,\"request_to_buy_enabled\":true,\"license_requests_enabled\":false,\"converted_bits\":0,\"editors_choice\":false,\"editors_choice_date\":null,\"feature\":\"fresh\",\"feature_date\":\"2018-01-16T22:28:47+00:00\",\"editored_by\":{},\"liked\":false,\"voted\":false,\"disliked\":false,\"purchased\":false,\"user\":{\"id\":25019057,\"username\":\"eupleridae\",\"firstname\":\"Anna\",\"lastname\":\"Olson\",\"city\":null,\"country\":null,\"usertype\":0,\"fullname\":\"Anna Olson\",\"userpic_url\":\"https://secure.gravatar.com/avatar/ed0be571358801b96e440655ae792edb?s=300&r=g&d=https://pacdn.500px.org/userpic.png\",\"userpic_https_url\":\"https://secure.gravatar.com/avatar/ed0be571358801b96e440655ae792edb?s=300&r=g&d=https://pacdn.500px.org/userpic.png\",\"cover_url\":null,\"upgrade_status\":4,\"store_on\":false,\"affection\":49,\"followers_count\":0,\"avatars\":{\"default\":{\"https\":\"https://secure.gravatar.com/avatar/ed0be571358801b96e440655ae792edb?s=300&r=g&d=https://pacdn.500px.org/userpic.png\"},\"large\":{\"https\":\"https://secure.gravatar.com/avatar/ed0be571358801b96e440655ae792edb?s=100&r=g&d=https://pacdn.500px.org/userpic.png\"},\"small\":{\"https\":\"https://secure.gravatar.com/avatar/ed0be571358801b96e440655ae792edb?s=50&r=g&d=https://pacdn.500px.org/userpic.png\"},\"tiny\":{\"https\":\"https://secure.gravatar.com/avatar/ed0be571358801b96e440655ae792edb?s=30&r=g&d=https://pacdn.500px.org/userpic.png\"}}},\"comments\":[],\"watermark\":false,\"licensing_requested\":false,\"licensing_suggested\":false,\"is_free_photo\":false},\"comments\":[]}";

    FiveHundredPxPhotoResponse photoResponse = MAPPER.readValue(photoString, FiveHundredPxPhotoResponse.class);

    MAPPER.writeValue(System.out, photoResponse);
    assertThat(photoResponse.getPhoto().getId()).isEqualTo(242773583);
  }
}
