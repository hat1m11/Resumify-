package org.hatim.resumify;

import java.util.*;

public class WordSimilarity {

    public String cleanText(String input) {
        return input.toLowerCase().replaceAll("[^a-z0-9 ]", " ").trim();
    }

    public String[] tokenize(String text) {
        return text.split("\\s+");
    }

    public Map<String, Integer> calcTermFrequency(String[] tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0) + 1);
        }
        return tf;
    }

    public Map<String, Double> calculateIDF(Map<String, Integer> resumeTF, Map<String, Integer> jdTF) {
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(resumeTF.keySet());
        vocabulary.addAll(jdTF.keySet());

        Map<String, Double> idfMap = new HashMap<>();
        int totalDocs = 2;

        for (String word : vocabulary) {
            int df = 0;
            if (resumeTF.containsKey(word)) df++;
            if (jdTF.containsKey(word)) df++;

            double idf = Math.log((double) totalDocs / (1 + df));
            idfMap.put(word, idf);
        }

        return idfMap;
    }

    public double calculateCosineSimilarity(Map<String, Integer> resumeTF,
                                            Map<String, Integer> jdTF,
                                            Map<String, Double> idfMap) {
        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(resumeTF.keySet());
        vocabulary.addAll(jdTF.keySet());

        double dotProduct = 0.0;
        double resumeNorm = 0.0;
        double jdNorm = 0.0;

        for (String word : vocabulary) {
            double resumeWeight = resumeTF.getOrDefault(word, 0) * idfMap.getOrDefault(word, 0.0);
            double jdWeight = jdTF.getOrDefault(word, 0) * idfMap.getOrDefault(word, 0.0);

            dotProduct += resumeWeight * jdWeight;
            resumeNorm += Math.pow(resumeWeight, 2);
            jdNorm += Math.pow(jdWeight, 2);
        }

        if (resumeNorm == 0 || jdNorm == 0) return 0.0;

        return dotProduct / (Math.sqrt(resumeNorm) * Math.sqrt(jdNorm));
    }
}
