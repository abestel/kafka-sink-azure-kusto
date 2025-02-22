package com.microsoft.azure.kusto.kafka.connect.sink.formatWriter;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;

import com.microsoft.azure.kusto.kafka.connect.sink.Utils;
import com.microsoft.azure.kusto.kafka.connect.sink.format.RecordWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AvroRecordWriterTest {
    @Test
    public void AvroDataWrite() throws IOException {
        List<SinkRecord> records = new ArrayList<>();
        final Schema schema = SchemaBuilder.struct()
                .field("text", SchemaBuilder.string().build())
                .field("id", SchemaBuilder.int32().build())
                .build();

        for (int i = 0; i < 10; i++) {
            final Struct struct = new Struct(schema)
                    .put("text", String.format("record-%s", i))
                    .put("id", i);
            records.add(new SinkRecord("mytopic", 0, null, null, schema, struct, 10));
        }
        File file = new File("abc.avro");
        Utils.restrictPermissions(file);
        AvroRecordWriterProvider writer = new AvroRecordWriterProvider();
        OutputStream out = Files.newOutputStream(file.toPath());
        RecordWriter rd = writer.getRecordWriter(file.getPath(), out);
        for (SinkRecord record : records) {
            rd.write(record);
        }
        rd.commit();
        validate(file.getPath());
        FileUtils.deleteQuietly(file);
    }

    public void validate(String path) throws IOException {
        // Warns if the types are not generified
        GenericDatumReader<GenericData.Record> datum = new GenericDatumReader<>();
        File file = new File(path);
        DataFileReader<GenericData.Record> reader = new DataFileReader<>(file, datum);
        GenericData.Record record = new GenericData.Record(reader.getSchema());
        int i = 0;
        while (reader.hasNext()) {
            assertEquals(reader.next(record).toString(), String.format("{\"text\": \"record-%s\", \"id\": %s}", i, i));
            i++;
        }
        reader.close();
    }
}
