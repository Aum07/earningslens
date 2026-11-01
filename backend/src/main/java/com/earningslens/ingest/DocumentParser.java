package com.earningslens.ingest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Turns a downloaded file into ordered blocks of text. HtmlParser now, PdfParser in E1.4. */
public interface DocumentParser {

    List<ParsedBlock> parse(Path file) throws IOException;
}
