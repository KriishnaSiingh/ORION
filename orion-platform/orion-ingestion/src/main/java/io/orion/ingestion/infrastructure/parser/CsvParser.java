package io.orion.ingestion.infrastructure.parser;

import com.opencsv.CSVReader;
import java.io.*;
import java.util.*;

public class CsvParser {
    public List<String> detectHeaders(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] headers = reader.readNext();
            return headers != null ? Arrays.asList(headers) : Collections.emptyList();
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Error reading CSV headers", e);
        }
    }

    public List<Map<String, String>> parse(File file) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] headers = reader.readNext();
            if (headers == null) return Collections.emptyList();

            List<Map<String, String>> data = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < Math.min(headers.length, line.length); i++) {
                    row.put(headers[i], line[i]);
                }
                data.add(row);
            }
            return data;
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Error parsing CSV file", e);
        }
    }
}
