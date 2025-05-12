package org.hatim.resumify;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.*;

public class DocReader {

    public String importDOC(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()){

            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();

            AutoDetectParser parser = new AutoDetectParser();
            parser.parse(inputStream, handler, metadata, parseContext);

            return handler.toString();

        } catch (IOException | SAXException | org.apache.tika.exception.TikaException e) {
            e.printStackTrace();
            return "Error parsing file.";
        }
    }
}
