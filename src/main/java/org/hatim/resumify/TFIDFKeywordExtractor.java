package org.hatim.resumify;

import java.util.*;
import java.util.stream.Collectors;

public class TFIDFKeywordExtractor {

    // Split, clean, and tokenize text
    private List<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(token -> token.length() > 2) // filter out short/irrelevant words
                .collect(Collectors.toList());
    }

    // Term Frequency (TF)
    private Map<String, Double> computeTF(List<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        double total = tokens.size();

        for (String word : tokens) {
            tf.put(word, tf.getOrDefault(word, 0.0) + 1.0);
        }

        tf.replaceAll((k, v) -> v / total);
        return tf;
    }

    // Inverse Document Frequency (IDF)
    private Map<String, Double> computeIDF(List<List<String>> allDocs) {
        Map<String, Double> idf = new HashMap<>();
        int totalDocs = allDocs.size();

        for (List<String> doc : allDocs) {
            Set<String> uniqueWords = new HashSet<>(doc);
            for (String word : uniqueWords) {
                idf.put(word, idf.getOrDefault(word, 0.0) + 1.0);
            }
        }

        idf.replaceAll((k, v) -> Math.log(totalDocs / v));
        return idf;
    }

    // TF-IDF score for each word in job description
    public List<String> getImportantMissingKeywords(String resumeText, String jdText, int topN) {
        List<String> resumeTokens = tokenize(resumeText);
        List<String> jdTokens = tokenize(jdText);

        Map<String, Double> tfJD = computeTF(jdTokens);
        Map<String, Double> idf = computeIDF(List.of(resumeTokens, jdTokens));

        Map<String, Double> tfidf = new HashMap<>();
        for (String word : jdTokens) {
            if (!resumeTokens.contains(word)) {
                double score = tfJD.getOrDefault(word, 0.0) * idf.getOrDefault(word, 0.0);
                tfidf.put(word, score);
            }
        }

        return tfidf.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
