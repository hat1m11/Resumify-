package org.hatim.resumify;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class HomeController {

    @RequestMapping("/home")
    public String test() {
        return "index"; // Return the 'index' view when accessing /home
    }

    @PostMapping("/result") // Handles POST requests to /result
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam("description") String description,
                         Model model) {

        if (file.isEmpty() || description.isBlank()) {
            model.addAttribute("message", "Resume or job description is missing.");
            return "errorPage";
        }

        // Process the file using DocReader
        DocReader reader = new DocReader();
        String content = reader.importDOC(file);



        JobDescription jobDescription = new JobDescription(description);

        // Add the content to the model to display in the result page
        model.addAttribute("content", content);
        model.addAttribute("jobDescription", jobDescription.getDescription());

        // Return 'result' view after processing the file
        return "result";
    }
}
