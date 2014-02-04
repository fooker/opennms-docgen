package org.opennms.docgen;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.RootDoc;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ServiceLoader;
import net.sourceforge.jwbf.core.contentRep.Article;
import net.sourceforge.jwbf.mediawiki.bots.MediaWikiBot;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.helpers.MessageFormatter;

public class WikiDoclet extends Doclet {

    private WikiDoclet() {
    }

    private static String wikiURL;

    private static String wikiUsername;
    private static String wikiPassword;

    private static String wikiNamespace = "Spec";

    public static int optionLength(final String name) {
        switch (name) {
            case "-wikiurl": return 2;
            case "-wikiuser": return 2;
            case "-wikipass": return 2;
            case "-wikinamespace": return 2;
            default: return Doclet.optionLength(name);
        }
    }

    private static boolean validOption(final String[] option) {
        switch (option[0]) {
            case "-wikiurl": return (wikiURL = option[1]) != null;
            case "-wikiuser": return (wikiUsername = option[1]) != null;
            case "-wikipass": return (wikiPassword = option[1]) != null;
            case "-wikinamespace": return (wikiNamespace = option[1]) != null;
            default: return false;
        }
    }

    public static boolean validOptions(final String[][] options,
                                       final DocErrorReporter der) {
        for (final String[] option : options) {
            validOption(option);
        }

        return Doclet.validOptions(options,
                                   der);
    }

    private static RootDoc root;

    public static boolean start(final RootDoc root) {
        WikiDoclet.root = root;

        // Create the template engine
        final VelocityEngine ve = new VelocityEngine();
        ve.addProperty("resource.loader",
                       "class");
        ve.addProperty("class.resource.loader.class",
                       "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        ve.init();

        // Initialize the mediawiki bot
        final MediaWikiBot bot = new MediaWikiBot(wikiURL);
        bot.login(wikiUsername,
                  wikiPassword);

        // Find and load all inspector implementations
        final ServiceLoader<Inspector> inspectors = ServiceLoader.load(Inspector.class);

        // Let each inspector process all classes
        for (final Inspector inspector
             : inspectors) {
            logDebug("Running inspector: {}",
                     inspector.getClass().getName());

            // Find the base class
            final ClassDoc baseClass = root.classNamed(inspector
                    .getBaseClassName());
            if (baseClass == null) {
                logWarning("Unable to find base class {} from inspector {} - skipping",
                           inspector.getBaseClassName(),
                           inspector.getClass().getName());
                continue;
            }

            // Load the template for the inspector
            final Template template = ve.getTemplate("templates/" + inspector.getTemplate());

            // Process all top-level classes
            for (final ClassDoc clazz : root.classes()) {
                logDebug("Processing class: {}",
                         clazz.name());

                // Skip all classes being abstract or not inheriting or
                // implementing the base class
                if (clazz.isAbstract() || !clazz.subclassOf(baseClass)) {
                    continue;
                }

                logDebug("Generating class: {}",
                         clazz.name());

                // Calculate the page name
                final String page = wikiNamespace + ":" + inspector.buildPageName(clazz);

                // Build a context for this class
                final VelocityContext context = new VelocityContext();
                context.put("doclet",
                            WikiDoclet.class);
                context.put("inspector",
                            inspector);
                context.put("class",
                            clazz);
                context.put("page",
                            page);

                try (final StringWriter buffer = new StringWriter()) {
                    logDebug("Rendering class: {}",
                             clazz.name());

                    // Render the template using the context to the buffer
                    template.merge(context,
                                   buffer);

                    logDebug("Uploading page: {}",
                             page);

                    // Get the article to write to, fill it with the content
                    // from the template and store it
                    final Article article = bot.getArticle(page);
                    article.setText(buffer.toString());
                    article.save("Updated from source code");
                    
                    System.out.println(buffer.toString());

                } catch (IOException ex) {
                    logError("Failed to render template: {}",
                             ex.getMessage());
                }
            }
        }

        return true;
    }

    public static void logDebug(String message,
                                Object... arguments) {
        WikiDoclet.root.printNotice(MessageFormatter.arrayFormat(message,
                                                                 arguments).getMessage());
    }

    public static void logWarning(String message,
                                  Object... arguments) {
        WikiDoclet.root.printWarning(MessageFormatter.arrayFormat(message,
                                                                  arguments).getMessage());
    }

    public static void logError(String message,
                                Object... arguments) {
        WikiDoclet.root.printError(MessageFormatter.arrayFormat(message,
                                                                arguments).getMessage());
    }

}
