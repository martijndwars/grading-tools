package org.metaborg.spt.listener.grading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.Iterator;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.metaborg.spt.listener.ITestReporter;
import org.metaborg.spt.listener.TestReporterProvider;
import org.metaborg.sunshine.Environment;
import org.metaborg.sunshine.drivers.SunshineMainArguments;
import org.metaborg.sunshine.drivers.SunshineMainDriver;
import org.metaborg.sunshine.services.language.ALanguage;
import org.metaborg.sunshine.services.language.LanguageDiscoveryService;
import org.metaborg.sunshine.services.language.LanguageService;
import org.spoofax.interpreter.terms.IStrategoAppl;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.terms.TermFactory;
import org.spoofax.terms.io.binary.TermReader;

public class Runner {

	
	public static void main(String[] args) {
	
		int i = 0;
		final CompositeConfiguration testConfig = new CompositeConfiguration();
		try {
			final PropertiesConfiguration sptConfig = new PropertiesConfiguration("spt.properties");
			
			testConfig.addConfiguration(sptConfig);
			testConfig.addConfiguration(new PropertiesConfiguration("tests.properties"));
						
			register(sptConfig.getFile().getParentFile().getPath() + "/" + testConfig.getString("spt.esv"));
			final String tests = testConfig.getString("tests");
			final String builder = testConfig.getString("spt.builder");

			XMLConfiguration langConfig = new XMLConfiguration("languages.xml");
			
			i += runGroup(langConfig.getFile().getParentFile().getPath(), langConfig, tests, builder); 
			
		} catch (ConfigurationException e) {
			e.printStackTrace();
			i = 1;
		}
		
		for (Iterator<ITestReporter> reporters = TestReporterProvider.getInstance().getReporters(); reporters.hasNext();) {
			ITestReporter reporter = reporters.next();
			if (reporter instanceof Grader) {
				Grader grader = (Grader) reporter;
				final int valid = grader.getValid();
				final int effective = grader.getEffective();
				System.out.println("valid " + valid);
				System.out.println("invalid " + grader.getInvalid());
				System.out.println("effective " + effective);
				System.out.println("ineffective " + (valid-effective));
			}
		}
		
		System.exit(i);
	}
	
	public static int runGroup(String project, HierarchicalConfiguration config,
			final String tests, final String builder) {
		
		int i = 0;
		for (String variant: config.getStringArray("language[@esv]")) {
			for (Iterator<ITestReporter> reporters = TestReporterProvider.getInstance().getReporters(); reporters.hasNext();) {
				ITestReporter reporter = reporters.next();
				if (reporter instanceof Grader) {
					Grader grader = (Grader) reporter;
					grader.setLanguage(variant);
				}
			}
			
			i += runTests(project + "/" + variant, tests, builder); 
			
			for (Iterator<ITestReporter> reporters = TestReporterProvider.getInstance().getReporters(); reporters.hasNext();) {
				ITestReporter reporter = reporters.next();
				if (reporter instanceof Grader) {
					Grader grader = (Grader) reporter;
					if (grader.isDetected())
						System.out.println("detected " + project + variant);
					else
						System.out.println("undetected " + project + variant);
				}
			}
		}
		int g = 0;
		for (Object group: config.getList("group[@name]")) {
			System.out.println("Group " + group);
			i += runGroup(project, config.configurationAt("group(" + g++ +")"), tests, builder);
		}
		return i;
	}
	
	public static int runTests(String language, String project, String builder) {
	
		try {
			FileUtils.deleteDirectory(new File(project+".cache"));
		} catch (IOException e) {}
	 
		SunshineMainArguments params = new SunshineMainArguments();
		params.builder = builder;
		params.filestobuildon = ".";
		params.noanalysis = true;
		
		Environment env = Environment.INSTANCE();
		env.setMainArguments(params);
		env.setProjectDir(new File(project));
		
		register(language);
		
//		LanguageService.INSTANCE().registerLanguage(
//					LanguageDiscoveryService.INSTANCE().languageFromArguments(params.languageArgs));
		
		if (new SunshineMainDriver().run() == 0)
			return 0;
		else 
			return 1;
	}

	public static void register(String esv) {
		
		IStrategoAppl document = null;

		try {
			PushbackInputStream input = new PushbackInputStream(new FileInputStream(esv), 100);
			byte[] buffer = new byte[6];
			int bufferSize = input.read(buffer);
			if (bufferSize != -1)
				input.unread(buffer, 0, bufferSize);
			
			if ((bufferSize == 6 && new String(buffer).equals("Module"))) { 
				TermReader reader = new TermReader(
						new TermFactory().getFactoryWithStorageType(IStrategoTerm.MUTABLE));
				document = (IStrategoAppl) reader.parseFromStream(input);
			} 
			
			ALanguage lang = LanguageDiscoveryService.INSTANCE().languageFromEsv(document, new File(esv).toPath().getParent().getParent());
			LanguageService.INSTANCE().registerLanguage(lang);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load language", e); 
		}
	}
}
