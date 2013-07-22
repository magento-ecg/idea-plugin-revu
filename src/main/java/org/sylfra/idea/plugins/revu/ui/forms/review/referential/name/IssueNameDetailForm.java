package org.sylfra.idea.plugins.revu.ui.forms.review.referential.name;

import com.intellij.ui.table.TableView;
import org.sylfra.idea.plugins.revu.model.IssueName;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.AbstractNameHolderDetailForm;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class IssueNameDetailForm extends AbstractNameHolderDetailForm<IssueName>
{
    protected IssueNameDetailForm(TableView<IssueName> table)
    {
        super(table);
    }
}