package org.opennms.docgen.inspectors;

import com.sun.javadoc.ClassDoc;
import org.opennms.docgen.AbstractBeanInspector;

public class DetectorInspector extends AbstractBeanInspector {

    @Override
    public String getBaseClassName() {
        return "org.opennms.netmgt.provision.ServiceDetector";
    }

    @Override
    public String getTemplate() {
        return "detector.vm";
    }

    @Override
    public String buildPageName(final ClassDoc clazz) {
        return clazz.name();
    }
}
