package org.sylfra.idea.plugins.revu.externalizing.impl;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.sylfra.idea.plugins.revu.model.DataReferential;
import org.sylfra.idea.plugins.revu.model.IssuePriority;
import org.sylfra.idea.plugins.revu.model.IssueName;
import org.sylfra.idea.plugins.revu.model.IssueTag;
import org.sylfra.idea.plugins.revu.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
class DataReferentialConverter extends AbstractConverter
{
  public boolean canConvert(Class type)
  {
    return DataReferential.class.equals(type);
  }

  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context)
  {
    DataReferential referential = (DataReferential) source;

    // Priorities
    writer.startNode("priorities");
    SortedSet<IssuePriority> priorities = new TreeSet<IssuePriority>(
      referential.getIssuePrioritiesByName(false).values());
    for (IssuePriority priority : priorities)
    {
      writer.startNode("priority");
      context.convertAnother(priority);
      writer.endNode();
    }
    writer.endNode();

      // Names
      writer.startNode("issueNames");
      SortedSet<IssueName> issueNames = new TreeSet<IssueName>(
              referential.getIssueNamesByName(false).values());
      for (IssueName issueName : issueNames) {
          writer.startNode("issueName");
          context.convertAnother(issueName);
          writer.endNode();
      }
      writer.endNode();

    // Tags
    writer.startNode("tags");
    SortedSet<IssueTag> tags = new TreeSet<IssueTag>(
      referential.getIssueTagsByName(false).values());
    for (IssueTag tag : tags)
    {
      writer.startNode("tag");
      context.convertAnother(tag);
      writer.endNode();
    }
    writer.endNode();

    // Users
    writer.startNode("users");
    SortedSet<User> users = new TreeSet<User>(referential.getUsersByLogin(false).values());
    for (User user : users)
    {
      writer.startNode("user");
      context.convertAnother(user);
      writer.endNode();
    }
    writer.endNode();
  }

  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context)
  {
    DataReferential referential = new DataReferential(getReview(context));

    while (reader.hasMoreChildren())
    {
      reader.moveDown();
      if ("priorities".equals(reader.getNodeName()))
      {
        List<IssuePriority> priorities = new ArrayList<IssuePriority>();
        while (reader.hasMoreChildren())
        {
          reader.moveDown();
          priorities.add((IssuePriority) context.convertAnother(priorities, IssuePriority.class));
          reader.moveUp();
        }
        referential.setIssuePriorities(priorities);
      }
        else if ("issueNames".equals(reader.getNodeName())) {
            List<IssueName> issueNames = new ArrayList<IssueName>();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                issueNames.add((IssueName) context.convertAnother(issueNames, IssueName.class));
                reader.moveUp();
            }
            referential.setIssueNames(issueNames);
        }
      else if ("tags".equals(reader.getNodeName()))
      {
        List<IssueTag> tags = new ArrayList<IssueTag>();
        while (reader.hasMoreChildren())
        {
          reader.moveDown();
          tags.add((IssueTag) context.convertAnother(tags, IssueTag.class));
          reader.moveUp();
        }
        referential.setIssueTags(tags);
      }
      else if ("users".equals(reader.getNodeName()))
      {
        List<User> users = new ArrayList<User>();
        while (reader.hasMoreChildren())
        {
          reader.moveDown();
          users.add((User) context.convertAnother(users, User.class));
          reader.moveUp();
        }
        referential.setUsers(users);
      }
      reader.moveUp();
    }

    return referential;
  }
}