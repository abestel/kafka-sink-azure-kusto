package com.microsoft.azure.kusto.kafka.connect.sink.formatWriter;

import com.microsoft.azure.kusto.kafka.connect.sink.format.RecordWriter;
import com.microsoft.azure.kusto.kafka.connect.sink.format.RecordWriterProvider;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ByteRecordWriterProvider implements RecordWriterProvider {
    private static final Logger log = LoggerFactory.getLogger(ByteRecordWriterProvider.class);

    @Override
    public RecordWriter getRecordWriter(String filename, OutputStream out) {
        return new RecordWriter() {

            @Override
            public void write(SinkRecord record) throws IOException {
                byte[] value = null;
                byte[] valueBytes = (byte[]) record.value();
                if (filename.contains("avro")) {
                    value = new byte[valueBytes.length];
                    System.arraycopy(valueBytes, 0, value, 0, valueBytes.length);
                } else {
                    byte[] separator = "\n".getBytes(StandardCharsets.UTF_8);
                    byte[] valueWithSeparator = new byte[valueBytes.length + separator.length];
                    System.arraycopy(valueBytes, 0, valueWithSeparator, 0, valueBytes.length);
                    System.arraycopy(separator, 0, valueWithSeparator, valueBytes.length, separator.length);
                    value = valueWithSeparator;
                }
                out.write(value);
            }

            @Override
            public void close() {
                try {
                    out.close();
                } catch (IOException e) {
                    throw new DataException(e);
                }
            }

            @Override
            public void commit() {
                try {
                    out.flush();
                } catch (IOException e) {
                    throw new DataException(e);
                }
            }
        };
    }
}
