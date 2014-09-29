package nl.tudelft.in4303.grading;

import nl.tudelft.in4303.grading.github.GitHubGrader;
import nl.tudelft.in4303.grading.language.LanguageGrader;
import nl.tudelft.in4303.grading.tests.TestsGrader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class Main {

	public static void main(String[] args) {

		try {
			PropertiesConfiguration user = new PropertiesConfiguration(
					"gh.properties");
			GitHubGrader grader = new GitHubGrader(user.getString("user"),
					user.getString("user2"));
			grader.registerRunner("assignment1", new TestsGrader());
			grader.registerRunner("assignment2", new LanguageGrader());
			grader.grade("^student-(.*)$");
			
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}
}
