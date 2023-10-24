package lucenex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class FileIndexer {

    public static void main(String[] args) {
        String indexPath = "target/idx11";  // Specifica il percorso dell'indice
        String docsPath = "C:\\Users\\georg\\Documents\\D184MB";  // Specifica il percorso della directory con i file .txt

        try {
            Directory directory = FSDirectory.open(FileSystems.getDefault().getPath(indexPath));
            Analyzer defaultAnalyzer = new StandardAnalyzer();
            CharArraySet stopWords = new CharArraySet(Arrays.asList("a", "an", "and", "the", "in", "on", "of", "with",
            "for", "as", "at", "by", "to", "is", "it", "that", "was", "were", "from", "or"), true);

            // Mappa gli analyzer ai campi specifici
            Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
            perFieldAnalyzers.put("nome", new KeywordAnalyzer());
            perFieldAnalyzers.put("contenuto", new EnglishAnalyzer(stopWords));

            // Configura l'IndexWriter
            IndexWriterConfig config = new IndexWriterConfig(new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers));
            config.setCodec(new SimpleTextCodec());
            IndexWriter writer = new IndexWriter(directory, config);
            writer.deleteAll();

            // Indicizza i file .txt nella directory specificata
            File dir = new File(docsPath);
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                	if (file.getName().toLowerCase().endsWith(".txt")) {
                	    String nomeDocumento = file.getName(); 
                	    Document document = new Document();
                	    document.add(new StringField("nome", nomeDocumento, Field.Store.YES));
                        // Leggi il contenuto del file .txt
                        StringBuilder contents = new StringBuilder();
                        try (Scanner scanner = new Scanner(file)) {
                            while (scanner.hasNextLine()) {
                                contents.append(scanner.nextLine()).append("\n");
                            }
                        }
                        document.add(new TextField("contenuto", contents.toString(), Field.Store.YES));

                        writer.addDocument(document);
                    }
                }
            }

            
            long startTime = System.currentTimeMillis();
            
            // Chiudi l'IndexWriter
            writer.commit();
            writer.close();
            
            long endTime = System.currentTimeMillis();
            long indexingTime = endTime - startTime;
            System.out.println("Tempo di indicizzazione: " + indexingTime + " millisecondi");

            System.out.println("Indicizzazione completata con successo.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}