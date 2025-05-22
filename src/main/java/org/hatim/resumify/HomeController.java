package org.hatim.resumify;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class HomeController {

    @RequestMapping("/home")
    public String test() {
        return "index";
    }

    @PostMapping("/result")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("description") String description,
                         Model model) throws IOException {

        if (file.isEmpty() || description.isBlank()) {
            model.addAttribute("message", "Resume or job description is missing.");
            return "errorPage";
        }

        // Read file
        DocReader reader = new DocReader();  // Ensure this is correct
        String content = reader.importDOC(file);

        // Use WordSimilarity
        WordSimilarity ws = new WordSimilarity();  // Ensure this is correct

        String cleanContent = ws.cleanText(content);
        String cleanDescription = ws.cleanText(description);

        String[] contentTokens = ws.tokenize(cleanContent);
        String[] descriptionTokens = ws.tokenize(cleanDescription);

        var resumeTF = ws.calcTermFrequency(contentTokens);
        var jdTF = ws.calcTermFrequency(descriptionTokens);
        var idf = ws.calculateIDF(resumeTF, jdTF);

        double similarity = ws.calculateCosineSimilarity(resumeTF, jdTF, idf);

        KeywordExtractor wordExtract = new KeywordExtractor();
        List<String> missingKeywords = (List<String>) wordExtract.getMissingKeywords(cleanContent, cleanDescription, 15);

        model.addAttribute("missingKeywords", missingKeywords);
        model.addAttribute("content", cleanContent);
        model.addAttribute("jobDescription", cleanDescription);
        model.addAttribute("similarityScore", String.format("%.2f", similarity * 100) + "% match");

        return "result";
    }
}
