package org.sylfra.idea.plugins.revu.model;

import org.jetbrains.annotations.NotNull;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class IssueName extends AbstractRevuEntity<IssueName> implements Comparable<IssueName>,
        IRevuUniqueNameHolderEntity<IssueName>
{
    private int order;
    private String name;
    private String description;
    private String recommendation;
    private String priority;
    private String tags;

    public IssueName()
    {
    }

    public IssueName(int order, String name, String description, String recommendation, String priority, String tags)
    {
        this.order = order;
        this.name = name;
        this.description = description;
        this.recommendation = recommendation;
        this.priority = priority;
        this.tags = tags;
    }

    public int getOrder()
    {
        return order;
    }

    public void setOrder(int order)
    {
        this.order = order;
    }

    public String getName()
    {
        return name;
    }

    public void setName(@NotNull String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(@NotNull String description)
    {
        this.description = description;
    }

    public String getRecommendation()
    {
        return recommendation;
    }

    public String getPriority()
    {
        return priority;
    }

    public String getTags()
    {
        return tags;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(IssueName o)
    {
        return order - o.order;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IssueName issueName = (IssueName) o;

        if (order != issueName.order) {
            return false;
        }
        if (name != null ? !name.equals(issueName.name) : issueName.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = (int) order;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
