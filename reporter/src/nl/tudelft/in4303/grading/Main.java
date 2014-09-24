package nl.tudelft.in4303.grading;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import nl.tudelft.in4303.grading.github.GitHubGrader;
import nl.tudelft.in4303.grading.tests.TestsGrader;

public class Main {

	public static void main(String[] args) {

		try {
			PropertiesConfiguration user = new PropertiesConfiguration(
					"gh.properties");
			GitHubGrader grader = new GitHubGrader(user.getString("user"),
					user.getString("user2"));
			grader.registerRunner("assignment1", new FeedbackWrapper(new TestsGrader("languages2.xml")));
			grader.check("^student-pmit(.*)$");
			
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

	}
}