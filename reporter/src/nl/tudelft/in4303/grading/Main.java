package nl.tudelft.in4303.grading;

import com.beust.jcommander.JCommander;
import nl.tudelft.in4303.grading.github.GitHubGrader;
import nl.tudelft.in4303.grading.language.LanguageGrader;
import nl.tudelft.in4303.grading.tests.TestsGrader;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;

public class Main {

    public static void main(String[] args) throws Exception {

        CLOptions options = new CLOptions();
        new JCommander(options, args);

        try {
            GitHubGrader grader = GitHubGrader.fromConfig("gh.properties", options.runDry());

            Grader reporter;

            if (ConfigurationUtils.locate(options.getSolution(), "tests.xml") != null) {
                reporter = new LanguageGrader(options.getSolution());
            } else {
                reporter = new TestsGrader(options.getSolution());
            }

            String pattern = "^student-" + options.getStudent() + "(.*)$";
            if (options.reportDetails())
                grader.grade(reporter, options.getBranch(), pattern);
            else
                grader.feedback(reporter, options.getBranch(), pattern, options.getLate());

        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }
}
