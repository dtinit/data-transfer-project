package org.datatransferproject.transfer.koofr.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;

public class Fixtures {
  public static List<FilesListRecursiveItem> listRecursiveItems =
      ImmutableList.of(
          new FilesListRecursiveItem(
              "file",
              "/",
              new FilesFile(
                  "Data transfer", "dir", 1598267490866L, 0L, "", null, ImmutableMap.of()),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 1",
              new FilesFile("Album 1", "dir", 1598267493756L, 0L, "", null, ImmutableMap.of()),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 1/Photo 1.jpg",
              new FilesFile(
                  "Photo 1.jpg",
                  "file",
                  1324824491000L,
                  59339L,
                  "image/jpeg",
                  "bdd24ca54fdd485a6dc10cd7571c3bb0",
                  ImmutableMap.of("description", ImmutableList.of("Photo 1 description"))),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 1/Photo 2.jpg",
              new FilesFile(
                  "Photo 2.jpg",
                  "file",
                  1368774569000L,
                  331731L,
                  "image/jpeg",
                  "02b0cef9ebcfdd043d1452baf64e9eab",
                  ImmutableMap.of()),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 2 :heart:",
              new FilesFile(
                  "Album 2",
                  "dir",
                  1598267491759L,
                  0L,
                  "",
                  null,
                  ImmutableMap.of(
                      "description",
                      ImmutableList.of("Album 2 description ❤️"),
                      "originalName",
                      ImmutableList.of("Album 2 ❤️"))),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 2 :heart:/Photo 3.jpg",
              new FilesFile(
                  "Photo 3.jpg",
                  "file",
                  1489345497000L,
                  165309L,
                  "image/jpeg",
                  "4870ded22ee58d4dbc1d044763437aaa",
                  ImmutableMap.of("description", ImmutableList.of("Photo 3 description"))),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 2 :heart:/Video 1.mp4",
              new FilesFile(
                  "Video 1.mp4",
                  "file",
                  1599223257741L,
                  4642325L,
                  "video/mp4",
                  "1f710b4e476becd9a2fec72c4aa5ab7f",
                  ImmutableMap.of()),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Album 3",
              new FilesFile("Album 3", "dir", 1598267491759L, 0L, "", null, ImmutableMap.of()),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Videos",
              new FilesFile("Videos", "dir", 1599222806829L, 0L, "", null, ImmutableMap.of()),
              null),
          new FilesListRecursiveItem(
              "file",
              "/Videos/Video 2.mp4",
              new FilesFile(
                  "Video 2.mp4",
                  "file",
                  1599223266949L,
                  2644034L,
                  "video/mp4",
                  "8d0617e13a112b09a6f77b65d5abd57f",
                  ImmutableMap.of("description", ImmutableList.of("Video 3 description"))),
              null));
}
