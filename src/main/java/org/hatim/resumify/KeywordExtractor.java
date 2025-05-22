package org.hatim.resumify;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeywordExtractor {

    // Additional stop words commonly found in job descriptions but not meaningful for skills
    private static final Set<String> ADDITIONAL_STOP_WORDS = new HashSet<>(Arrays.asList(
            "job", "position", "candidate", "required", "role", "team", "company",
            "experience", "work", "ability", "looking", "strong", "opportunity",
            "knowledge", "skills", "qualified", "responsibilities", "requirements"
    ));

    // Technical terms commonly found in job descriptions
    private static final Set<String> TECH_TERMS = new HashSet<>(Arrays.asList(
            "java", "spring", "springboot", "hibernate", "sql", "nosql", "aws", "azure",
            "docker", "kubernetes", "microservices", "rest", "api", "json", "xml",
            "javascript", "typescript", "react", "angular", "vue", "node", "python",
            "scala", "kotlin", "go", "rust", "c++", "c#", ".net", "git", "devops",
            "ci/cd", "jenkins", "mongodb", "mysql", "postgresql", "oracle", "redis",
            "kafka", "rabbitmq", "elasticsearch", "hadoop", "spark", "machine learning",
            "ai", "artificial intelligence", "algorithms", "data structures", "agile",
            "scrum", "tdd", "test-driven", "restful", "oauth", "jwt", "security",
            "backend", "frontend", "fullstack", "web services", "linux", "unix", "shell"
    ));

    // Soft skills commonly sought in job descriptions
    private static final Set<String> SOFT_SKILLS = new HashSet<>(Arrays.asList(
            "communication", "teamwork", "leadership", "problem-solving", "self-motivated",
            "time management", "adaptability", "creativity", "critical thinking", "collaboration",
            "decision-making", "flexibility", "organizational", "prioritization", "attention to detail",
            "interpersonal", "analytical", "presentation", "verbal", "written", "multitasking",
            "client-facing", "mentoring", "negotiation", "conflict resolution", "customer-oriented"
    ));

    // Tokenize text using custom analyzer that keeps hyphenated terms intact
    private List<String> tokenize(String text) throws IOException {
        List<String> result = new ArrayList<>();

        // First, extract multi-word technical terms and soft skills
        Set<String> extractedPhrases = new HashSet<>();
        String lowerCaseText = text.toLowerCase();

        // Look for tech terms and soft skills phrases (ones with spaces)
        for (String term : TECH_TERMS) {
            if (term.contains(" ") && lowerCaseText.contains(term)) {
                extractedPhrases.add(term);
            }
        }

        for (String skill : SOFT_SKILLS) {
            if (skill.contains(" ") && lowerCaseText.contains(skill)) {
                extractedPhrases.add(skill);
            }
        }

        // Regular tokenization for single words
        Analyzer analyzer = new StandardAnalyzer();
        try (TokenStream stream = analyzer.tokenStream(null, new StringReader(text))) {
            CharTermAttribute attr = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                String token = attr.toString().toLowerCase();
                if (token.length() > 2 && !ADDITIONAL_STOP_WORDS.contains(token)) {
                    result.add(token);
                }
            }
            stream.end();
        }
        analyzer.close();

        // Add extracted phrases
        result.addAll(extractedPhrases);

        return result;
    }

    // Improved TF-IDF like calculation
    private Map<String, Double> computeTermImportance(List<String> tokens) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : tokens) {
            termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
        }

        // Compute importance score - give higher weight to technical terms and soft skills
        Map<String, Double> importance = new HashMap<>();
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String term = entry.getKey();
            int frequency = entry.getValue();

            double score = frequency;

            // Boost technical terms
            if (TECH_TERMS.contains(term)) {
                score *= 2.5;
            }

            // Boost soft skills
            if (SOFT_SKILLS.contains(term)) {
                score *= 2.0;
            }

            // Length boost for acronyms and meaningful terms
            if (term.length() <= 3 && term.toUpperCase().equals(term)) {
                score *= 1.5; // Likely an acronym
            } else if (term.length() >= 6) {
                score *= 1.2; // Likely a meaningful term
            }

            importance.put(term, score);
        }

        return importance;
    }

    // Extract keywords from job description
    public Map<String, Double> extractKeywords(String text) throws IOException {
        List<String> tokens = tokenize(text);
        return computeTermImportance(tokens);
    }

    // Extract top N keywords by computed importance
    public List<String> extractTopKeywords(String text, int topN) throws IOException {
        Map<String, Double> importance = extractKeywords(text);

        return importance.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Categorize keywords into technical skills, soft skills, etc.
    public Map<String, List<String>> categorizeKeywords(String text, int topN) throws IOException {
        Map<String, Double> allKeywords = extractKeywords(text);

        // Categorize by type
        Map<String, List<String>> categorized = new HashMap<>();
        categorized.put("technical_skills", new ArrayList<>());
        categorized.put("soft_skills", new ArrayList<>());
        categorized.put("other_keywords", new ArrayList<>());

        allKeywords.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .forEach(entry -> {
                    String keyword = entry.getKey();
                    if (TECH_TERMS.contains(keyword)) {
                        categorized.get("technical_skills").add(keyword);
                    } else if (SOFT_SKILLS.contains(keyword)) {
                        categorized.get("soft_skills").add(keyword);
                    } else {
                        categorized.get("other_keywords").add(keyword);
                    }
                });

        return categorized;
    }

    // Example: get missing keywords (words in JD but not in resume)
    public List<String> getMissingKeywords(String resumeText, String jdText, int topN) throws IOException {
        Map<String, Double> jdKeywords = extractKeywords(jdText);
        Map<String, Double> resumeKeywords = extractKeywords(resumeText);

        // Find missing keywords
        return jdKeywords.entrySet().stream()
                .filter(entry -> !resumeKeywords.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Get categorized missing keywords
    public Map<String, List<String>> getCategorizedMissingKeywords(String resumeText, String jdText, int topN) throws IOException {
        Map<String, Double> jdKeywords = extractKeywords(jdText);
        Map<String, Double> resumeKeywords = extractKeywords(resumeText);

        // Find missing keywords
        List<String> missingKeywords = jdKeywords.entrySet().stream()
                .filter(entry -> !resumeKeywords.containsKey(entry.getKey()))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Categorize missing keywords
        Map<String, List<String>> categorizedMissing = new HashMap<>();
        categorizedMissing.put("technical_skills", new ArrayList<>());
        categorizedMissing.put("soft_skills", new ArrayList<>());
        categorizedMissing.put("other_keywords", new ArrayList<>());

        for (String keyword : missingKeywords) {
            if (TECH_TERMS.contains(keyword)) {
                categorizedMissing.get("technical_skills").add(keyword);
            } else if (SOFT_SKILLS.contains(keyword)) {
                categorizedMissing.get("soft_skills").add(keyword);
            } else {
                categorizedMissing.get("other_keywords").add(keyword);
            }
        }

        return categorizedMissing;
    }

    // Get keywords match score (percentage of important JD keywords present in resume)
    public double getMatchScore(String resumeText, String jdText) throws IOException {
        Map<String, Double> jdKeywords = extractKeywords(jdText);
        Map<String, Double> resumeKeywords = extractKeywords(resumeText);

        double totalScore = 0.0;
        double matchedScore = 0.0;

        for (Map.Entry<String, Double> entry : jdKeywords.entrySet()) {
            String keyword = entry.getKey();
            Double importance = entry.getValue();

            totalScore += importance;
            if (resumeKeywords.containsKey(keyword)) {
                matchedScore += importance;
            }
        }

        return totalScore > 0 ? (matchedScore / totalScore) * 100.0 : 0.0;
    }
}