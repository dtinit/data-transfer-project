package org.dataportabilityproject.serviceProviders.flickr;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.RequestContext;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.photos.Photo;
import com.flickr4java.flickr.photos.PhotoList;
import com.flickr4java.flickr.photos.PhotosInterface;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.Photosets;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;
import com.flickr4java.flickr.util.AuthStore;
import com.flickr4java.flickr.util.FileAuthStore;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.shared.IOInterface;
import org.scribe.model.Token;
import org.scribe.model.Verifier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class FlickrPhotoService implements Exporter<PhotoAlbum>, Importer<PhotoAlbum> {
    private static final int PHOTO_SETS_PER_PAGE = 500;
    private static final int PHOTO_PER_PAGE = 50;
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty("user.home"), ".store/flickr_creds");
    private static final List<String> EXTRAS = ImmutableList.of("url_o", "o_dims", "original_format");

    private final Flickr flickr;
    private final PhotosetsInterface photosetsInterface;
    private final PhotosInterface photosInterface;
    private final Uploader uploader;
    private Auth auth;
    private final AuthStore authStore;

    public FlickrPhotoService(String apiKey, String apiSecret, IOInterface ioInterface)
            throws IOException {
        this(apiKey, apiSecret);
        this.auth = authorizeUser(ioInterface);
    }

    public FlickrPhotoService(String apiKey, String apiSecret, String nsid)
            throws IOException {
        this(apiKey, apiSecret);
        this.auth = authStore.retrieve(nsid);
        RequestContext.getRequestContext().setAuth(auth);
    }

    private FlickrPhotoService(String apiKey, String apiSecret)
            throws IOException {
        this.flickr = new Flickr(apiKey, apiSecret, new REST());
        this.photosetsInterface = flickr.getPhotosetsInterface();
        this.photosInterface = flickr.getPhotosInterface();
        this.uploader = flickr.getUploader();
        try {
            this.authStore = new FileAuthStore(DATA_STORE_DIR);
        } catch (FlickrException e) {
            throw new IOException(e);
        }
    }

    private Auth authorizeUser(IOInterface ioInterface) throws IOException {
        AuthInterface authInterface = flickr.getAuthInterface();
        Token token = authInterface.getRequestToken();
        String url = authInterface.getAuthorizationUrl(token, Permission.WRITE);
        String tokenKey = ioInterface.ask("Please enter the code from this url: " + url);
        Token requestToken = authInterface.getAccessToken(token, new Verifier(tokenKey));
        try {
            Auth auth = authInterface.checkToken(requestToken);
            RequestContext.getRequestContext().setAuth(auth);
            authStore.store(auth);
            return auth;
        } catch (FlickrException e) {
            throw new IOException("Problem verifying auth token", e);
        }
    }

    private List<Photoset> getAllPhotoSets() throws FlickrException {
        boolean hasMore = true;
        int page = 1;
        List<Photoset> allPhotoSets = new ArrayList<>();
        while (hasMore) {
            Photosets photoSetList = photosetsInterface.getList(auth.getUser().getId(), PHOTO_SETS_PER_PAGE, page, "");
            hasMore = photoSetList.getPage() != photoSetList.getPages() && !photoSetList.getPhotosets().isEmpty();
            allPhotoSets.addAll(photoSetList.getPhotosets());
            page++;
        }
        return allPhotoSets;
    }

    private List<Photo> getAllPhotosNotInSet() throws FlickrException {
        boolean hasMore = true;
        int page = 1;
        List<Photo> allPhotoSets = new ArrayList<>();
        RequestContext.getRequestContext().setExtras(EXTRAS);
        while (hasMore) {

            PhotoList<Photo> photoSetList = photosInterface.getNotInSet(PHOTO_PER_PAGE, page);
            hasMore = photoSetList.getPage() != photoSetList.getPages() && !photoSetList.isEmpty();
            allPhotoSets.addAll(photoSetList);
            page++;
        }
        RequestContext.getRequestContext().setExtras(ImmutableList.of());
        return allPhotoSets;
    }

    private List<Photo> getPhotosIn(Photoset photoset) throws FlickrException {
        boolean hasMore = true;
        int page = 1;
        List<Photo> allPhotoSets = new ArrayList<>();
        while (hasMore) {

            PhotoList<Photo> photoSetList = photosetsInterface.getPhotos(photoset.getId(), ImmutableSet.copyOf(EXTRAS),
                    0, PHOTO_PER_PAGE, page);
            hasMore = photoSetList.getPage() != photoSetList.getPages() && !photoSetList.isEmpty();
            allPhotoSets.addAll(photoSetList);
            page++;
        }
        RequestContext.getRequestContext().setExtras(ImmutableList.of());
        return allPhotoSets;
    }

    @Override
    public void importItem(PhotoAlbum photoAlbum) throws IOException {
        PhotosetsInterface photosetsInterface = flickr.getPhotosetsInterface();
        try {
            Photoset photoset = null;
            for (org.dataportabilityproject.dataModels.photos.Photo photo : photoAlbum.getPhotos()) {
                String photoId = uploadPhoto(photo);
                if (photoset == null) {
                    photoset = photosetsInterface.create("Copy of - " + photoAlbum.getName(),
                            photoAlbum.getDescription(), photoId);
                } else {
                    photosetsInterface.addPhoto(photoset.getId(), photoId);
                }
            }
        } catch (FlickrException e) {
            throw new IOException("Problem communicating with serviceProviders.flickr", e);
        }
    }

    @Override
    public Collection<PhotoAlbum> export() throws IOException {
        ImmutableList.Builder<PhotoAlbum> albumBuilder = ImmutableList.builder();

        try {
            for (Photoset photoset : getAllPhotoSets()) {
                List<Photo> oldPhotos = getPhotosIn(photoset);
                List<org.dataportabilityproject.dataModels.photos.Photo> newPhotos = oldPhotos.stream()
                        .map(FlickrPhotoService::toCommonPhoto)
                        .collect(Collectors.toList());
                albumBuilder.add(new PhotoAlbum(photoset.getTitle(), photoset.getDescription(), newPhotos));
            }

            List<Photo> unallocatedPhotos = getAllPhotosNotInSet();
            if (!unallocatedPhotos.isEmpty()) {
                List<org.dataportabilityproject.dataModels.photos.Photo> newPhotos = unallocatedPhotos.stream()
                        .map(FlickrPhotoService::toCommonPhoto)
                        .collect(Collectors.toList());
                albumBuilder.add(
                        new PhotoAlbum("Unallocated Photos", "Photos that were not in any photo set", newPhotos));
            }

        } catch (FlickrException e) {
            throw new IOException("Problem communicating with serviceProviders.flickr", e);
        }

        return albumBuilder.build();
    }

    private static org.dataportabilityproject.dataModels.photos.Photo toCommonPhoto(Photo p) {
        checkState(!Strings.isNullOrEmpty(p.getOriginalSize().getSource()), "photo %s had a null url", p.getId());
        return new org.dataportabilityproject.dataModels.photos.Photo(
                p.getTitle(),
                p.getOriginalSize().getSource(),
                p.getDescription(),
                toMimeType(p.getOriginalFormat()));
    }

    private InputStream getImageAsStream(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.connect();
        return conn.getInputStream();
    }

    private String uploadPhoto(org.dataportabilityproject.dataModels.photos.Photo photo)
            throws IOException, FlickrException {
        BufferedInputStream inStream = new BufferedInputStream(getImageAsStream(photo.getFetchableUrl()));
        UploadMetaData uploadMetaData = new UploadMetaData()
                .setAsync(false)
                .setPublicFlag(false)
                .setFriendFlag(false)
                .setFamilyFlag(false)
                .setTitle("copy of - " + photo.getTitle())
                .setDescription(photo.getDescription());
        return uploader.upload(inStream, uploadMetaData);
    }

    private static String toMimeType(String flickrFormat) {
        switch (flickrFormat) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            default:
                throw new IllegalArgumentException("Don't know how to map: " + flickrFormat);
        }
    }
}
