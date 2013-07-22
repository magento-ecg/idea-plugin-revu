package org.sylfra.idea.plugins.revu.externalizing.impl;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.sylfra.idea.plugins.revu.model.IssueName;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
class IssueNameConverter extends AbstractConverter
{
    public boolean canConvert(Class type)
    {
        return IssueName.class.equals(type);
    }

    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
    {
        IssueName issueName = (IssueName) source;

        writer.addAttribute("order", String.valueOf(issueName.getOrder()));
        writer.addAttribute("name", issueName.getName());
    }

    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
    {
        String order = reader.getAttribute("order");
        String name = reader.getAttribute("name");
        String description = reader.getAttribute("description");
        String recommendation = reader.getAttribute("recommendation");
        String priority = reader.getAttribute("priority");
        String tags = reader.getAttribute("tags");

        return new IssueName(Integer.valueOf(order), name, description, recommendation, priority, tags);
    }
}