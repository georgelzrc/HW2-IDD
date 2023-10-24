package uniroma3.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.Term;

public class QueryIndex {

    public static void main(String[] args) throws Exception {
        Path indexPath = Paths.get("target/idx10");  // Sostituisci con il percorso del tuo indice
        Directory directory = FSDirectory.open(indexPath);
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();

        // Crea un parser di query per il campo "nome" e "contenuto"
        QueryParser queryParserNome = new QueryParser("nome", analyzer);
        QueryParser queryParserContenuto = new QueryParser("contenuto", analyzer);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Seleziona il tipo di query da eseguire:");
            System.out.println("1. Match All Docs Query");
            System.out.println("2. TermQuery");
            System.out.println("3. PhraseQuery");
            System.out.println("4. BooleanQuery");
            System.out.println("5. QueryParser");
            System.out.print("Inserisci il numero corrispondente: ");

            String choice = br.readLine();

            switch (choice) {
                case "1":
                    testMatchAllDocsQuery(searcher);
                    break;
                case "2":
                    testTermQuery(searcher, queryParserNome, queryParserContenuto);
                    break;
                case "3":
                    testPhraseQuery(searcher, queryParserContenuto);
                    break;
                case "4":
                    testBooleanQuery(searcher, queryParserNome, queryParserContenuto);
                    break;
                case "5":
                    testQueryParser(searcher, queryParserNome, queryParserContenuto);
                    break;
                default:
                    System.out.println("Scelta non valida.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
    }

    private static void testMatchAllDocsQuery(IndexSearcher searcher) throws IOException {
        System.out.println("Esecuzione di una Match All Docs Query...");
        Query query = new MatchAllDocsQuery();
        executeAndPrintResults(searcher, query);
    }

    private static void testTermQuery(IndexSearcher searcher, QueryParser queryParserNome, QueryParser queryParserContenuto) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Inserisci un termine da cercare: ");
            String term = br.readLine();

            System.out.println("Esecuzione di una TermQuery nel campo 'nome'...");
            Query queryNome = new TermQuery(new Term("nome", term));
            executeAndPrintResults(searcher, queryNome);

            System.out.println("Esecuzione di una TermQuery nel campo 'contenuto'...");
            Query queryContenuto = new TermQuery(new Term("contenuto", term));
            executeAndPrintResults(searcher, queryContenuto);
        }
    }

    private static void testPhraseQuery(IndexSearcher searcher, QueryParser queryParserContenuto) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Inserisci una frase da cercare nel campo 'contenuto': ");
            String phrase = br.readLine();

            System.out.println("Esecuzione di una PhraseQuery nel campo 'contenuto'...");
            PhraseQuery phraseQuery = new PhraseQuery("contenuto", phrase.split(" "));
            executeAndPrintResults(searcher, phraseQuery);
        }
    }

    private static void testBooleanQuery(IndexSearcher searcher, QueryParser queryParserNome, QueryParser queryParserContenuto) throws IOException, ParseException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Inserisci due termini separati da AND o OR: ");
            String input = br.readLine();

            String[] parts = input.split("\\s+(?i)(and|or)\\s+");
            if (parts.length != 3) {
                System.out.println("Input non valido. Utilizza il formato 'term1 AND term2' o 'term1 OR term2'.");
                return;
            }

            String term1 = parts[0].trim();
            String operator = parts[1].trim().toUpperCase(); // Converti l'operatore in maiuscolo
            String term2 = parts[2].trim();


            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            Query query1 = null;
            Query query2 = null;

            operator.toUpperCase(); // Converti l'operatore in maiuscolo

            
            if (operator.equalsIgnoreCase("AND")) {
                System.out.println("Esecuzione di una BooleanQuery con operatore AND...");
                query1 = queryParserNome.parse(term1);
                query2 = queryParserNome.parse(term2);
                booleanQueryBuilder.add(query1, BooleanClause.Occur.MUST);
                booleanQueryBuilder.add(query2, BooleanClause.Occur.MUST);
            } else if (operator.equalsIgnoreCase("OR")) {
                System.out.println("Esecuzione di una BooleanQuery con operatore OR...");
                query1 = queryParserContenuto.parse(term1);
                query2 = queryParserContenuto.parse(term2);
                booleanQueryBuilder.add(query1, BooleanClause.Occur.SHOULD);
                booleanQueryBuilder.add(query2, BooleanClause.Occur.SHOULD);
            } else {
                System.out.println("Operatore non valido. Utilizza 'AND' o 'OR'.");
                return;
            }

            executeAndPrintResults(searcher, booleanQueryBuilder.build());
        }
    }

    private static void testQueryParser(IndexSearcher searcher, QueryParser queryParserNome, QueryParser queryParserContenuto) throws Exception {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Inserisci una query (campo:termine): ");
            String queryString = br.readLine();

            // Split della query in campo e termini
            String[] parts = queryString.split(":");
            if (parts.length != 2) {
                System.out.println("Sintassi non valida. Usa il formato 'campo:termine'.");
                return;
            }

            String field = parts[0].trim();
            String term = parts[1].trim();

            Query query;
            if (field.equalsIgnoreCase("nome")) {
                System.out.println("Esecuzione di una QueryParser nel campo 'nome'...");
                query = queryParserNome.parse(term);
            } else if (field.equalsIgnoreCase("contenuto")) {
                System.out.println("Esecuzione di una QueryParser nel campo 'contenuto'...");
                query = queryParserContenuto.parse(term);
            } else {
                System.out.println("Campo non valido. Utilizza 'nome' o 'contenuto'.");
                return;
            }

            executeAndPrintResults(searcher, query);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }


    private static void executeAndPrintResults(IndexSearcher searcher, Query query) throws IOException {
        TopDocs hits = searcher.search(query, 10);
        System.out.println("Numero di documenti corrispondenti: " + hits.totalHits);

        if (hits.scoreDocs.length == 0) {
            System.out.println("Nessun documento corrispondente trovato.");
            return;
        }

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = hits.scoreDocs[i];
            Document doc = searcher.doc(scoreDoc.doc);
            System.out.println("Doc " + scoreDoc.doc + ":");
            System.out.println("Nome: " + doc.get("nome"));
           // System.out.println("Contenuto: " + doc.get("contenuto"));
            System.out.println("Punteggio: " + scoreDoc.score);

        }
    }

}
