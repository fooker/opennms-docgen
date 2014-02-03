package org.opennms.docgen;

import com.sun.javadoc.ClassDoc;

public interface Inspector {
    
    /**
     * The base class of all classes to process.
     * 
     * This is the full-qualified class name of the base class all classes must
     * implement to be passed to this inspector.
     * 
     * @return the full-qualified class name
     */
    public abstract String getBaseClassName();
    
    /**
     * The path of the template.
     * 
     * This must returns the filename of the template used to render the content
     * (not including the {@literal template/} prefix.
     * 
     * @return the template path
     */
    public abstract String getTemplate();
    
    /**
     * Creates the page name to use for this class.
     * 
     * The page name must be calculated from the given class and will be used as
     * page name in the wiki to update.
     * 
     * @param clazz the class to calculate the page name from
     * 
     * @return the page name
     */
    public abstract String buildPageName(final ClassDoc clazz);
}
