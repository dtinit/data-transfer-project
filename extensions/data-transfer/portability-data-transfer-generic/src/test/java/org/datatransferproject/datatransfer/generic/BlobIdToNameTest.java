package org.datatransferproject.datatransfer.generic;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class BlobIdToNameTest {
  
  @Test
  public void simpleImportTest() throws Exception {
    BlobIdToName initial = new BlobIdToName();
    initial.add("key1", "value1");
    initial.add("key2", "value2");
    
    ObjectMapper objectMapper = new ObjectMapper();
    BlobIdToName afterSerDer = objectMapper.readValue(objectMapper.writeValueAsBytes(initial), BlobIdToName.class);
    assert(afterSerDer.get("key1").equals("value1"));
    assert(afterSerDer.get("key2").equals("value2"));
  }
}
