package com.microsoft.azure.kusto.kafka.connect.sink.formatWriter;

import com.microsoft.azure.kusto.kafka.connect.sink.format.RecordWriter;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AvroRecordWriterTest {

  @Test
  public void AvroDataWrite() {
    try {
      List<SinkRecord> records = new ArrayList<SinkRecord>();
      final Schema schema = SchemaBuilder.struct()
          .field("text", SchemaBuilder.string().build())
          .field("id", SchemaBuilder.int32().build())
          .build();

      for(int i=0;i<10;i++) {
        final Struct struct = new Struct(schema)
            .put("text", String.format("record-%s", i))
            .put("id", i);
        records.add(new SinkRecord("mytopic", 0, null, null, schema, struct, 10));
      }
      File file = new File("abc.avro");
      AvroRecordWriterProvider writer = new AvroRecordWriterProvider();
      FileOutputStream fos = new FileOutputStream(file);
      OutputStream out=fos;
      RecordWriter rd = writer.getRecordWriter(file.getPath(),out);
      for(SinkRecord record : records){
        rd.write(record);
      }
      rd.commit();
      validate(file.getPath());
      assertEquals(rd.getDataSize(),290);
      file.delete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void validate(String path) {
    try {
      GenericDatumReader datum = new GenericDatumReader();
      File file = new File(path);
      DataFileReader reader = new DataFileReader(file, datum);

      GenericData.Record record = new GenericData.Record(reader.getSchema());
      int i=0;
      while (reader.hasNext()) {
        assertEquals(reader.next(record).toString(),String.format("{\"text\": \"record-%s\", \"id\": %s}",i,i));
        i++;
      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

