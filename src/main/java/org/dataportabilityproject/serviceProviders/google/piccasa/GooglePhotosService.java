package org.dataportabilityproject.serviceProviders.google.piccasa;

import com.google.api.client.auth.oauth2.Credential;
import com.google.common.collect.ImmutableList;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.MediaContent;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.media.MediaStreamSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.photos.Photo;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;

public class GooglePhotosService implements Exporter<PhotoAlbum>, Importer<PhotoAlbum> {
    private static final String CLIENT_NAME = "Portability";

    private final PicasawebService service;

    public GooglePhotosService(Credential credential) {
        this.service = new PicasawebService(CLIENT_NAME);
        this.service.setOAuth2Credentials(credential);
    }

    @Override
    public Collection<PhotoAlbum> export() throws IOException {
        URL albumUrl = new URL("https://picasaweb.serviceProviders.google.com/data/feed/api/user/default?kind=album");

        UserFeed albumFeed;

        try {
            albumFeed = service.getFeed(albumUrl, UserFeed.class);
        } catch (ServiceException e) {
            throw new IOException("Problem making request to: " + albumUrl, e);
        }

        ImmutableList.Builder<PhotoAlbum> albums = ImmutableList.builder();

        for (GphotoEntry myAlbum : albumFeed.getEntries()) {
            // imgmax=d gets the original immage as per:
            // https://developers.google.com/picasa-web/docs/2.0/reference
            URL photosUrl = new URL("https://picasaweb.serviceProviders.google.com/data/feed/api/user/default/albumid/"
                    + myAlbum.getGphotoId() + "?imgmax=d");
            AlbumFeed photoFeed;
            try {
                photoFeed = service.getFeed(photosUrl, AlbumFeed.class);
            } catch (ServiceException e) {
                throw new IOException("Problem making request to: " + albumUrl, e);
            }

            ImmutableList.Builder<Photo> photos = ImmutableList.builder();
            for(GphotoEntry photo : photoFeed.getEntries()) {
                MediaContent mediaContent = (MediaContent) photo.getContent();
                photos.add(new Photo(
                        photo.getTitle().getPlainText(),
                        mediaContent.getUri(),
                        photo.getDescription().getPlainText(),
                        mediaContent.getMimeType().getMediaType()
                ));
            }
            albums.add(new PhotoAlbum(
                    myAlbum.getTitle().getPlainText(),
                    myAlbum.getDescription().getPlainText(),
                    photos.build()
            ));
        }

        return albums.build();
    }

    @Override
    public void importItem(PhotoAlbum album) throws IOException {
        AlbumEntry myAlbum = new AlbumEntry();

        myAlbum.setTitle(new PlainTextConstruct("copy of " + album.getName()));
        myAlbum.setDescription(new PlainTextConstruct(album.getDescription()));

        URL albumUrl = new URL("https://picasaweb.serviceProviders.google.com/data/feed/api/user/default");
        AlbumEntry insertedEntry;

        try {
            // https://developers.google.com/picasa-web/docs/2.0/developers_guide_java#AddAlbums
            insertedEntry = service.insert(albumUrl, myAlbum);
        } catch (ServiceException e) {
            throw new IOException("Problem copying" +  album.getName() + " request to: " + albumUrl, e);
        }

        for (Photo photo : album.getPhotos()) {
            URL photoPostUrl = new URL("https://picasaweb.serviceProviders.google.com/data/feed/api/user/default/albumid/"
                    + insertedEntry.getGphotoId());

            PhotoEntry myPhoto = new PhotoEntry();
            myPhoto.setTitle(new PlainTextConstruct("copy of " + photo.getTitle()));
            myPhoto.setDescription(new PlainTextConstruct(photo.getDescription()));
            myPhoto.setClient(CLIENT_NAME);

            MediaStreamSource streamSource = new MediaStreamSource(getImageAsStream(photo.getFetchableUrl()),
                    photo.getMediaType());
            myPhoto.setMediaSource(streamSource);

            try {
                service.insert(photoPostUrl, myPhoto);
            } catch (ServiceException e) {
                throw new IOException("Problem adding " + photo.getTitle() + " to "
                        +  album.getName(), e);
            }
        }
    }

    private static InputStream getImageAsStream(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.connect();
        return conn.getInputStream();
    }
}
