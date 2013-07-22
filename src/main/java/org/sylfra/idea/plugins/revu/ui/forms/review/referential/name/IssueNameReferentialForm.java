package org.sylfra.idea.plugins.revu.ui.forms.review.referential.name;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.sylfra.idea.plugins.revu.model.IssueName;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.AbstractNameHolderReferentialForm;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.AbstractReferentialDetailForm;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.ReferentialListHolder;

import java.util.List;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class IssueNameReferentialForm extends AbstractNameHolderReferentialForm<IssueName> {

    public IssueNameReferentialForm(Project project) {
        super(project);
    }

    protected boolean isTableSelectionMovable() {
        return true;
    }

    protected AbstractReferentialDetailForm<IssueName> buildNestedFormForDialog()
    {
        return new IssueNameDetailForm(table);
    }

    @Nls
    protected String getTitleKeyForDialog(boolean addMode)
    {
        return addMode
                ? "projectSettings.review.referential.issueName.addDialog.title"
                : "projectSettings.review.referential.issueName.editDialog.title";
    }

    @NotNull
    protected IssueName createDefaultDataForDialog()
    {
        return new IssueName();
    }

    @Override
    protected void internalUpdateData(@NotNull ReferentialListHolder<IssueName> data)
    {
        super.internalUpdateData(data);

        List<IssueName> issueNames = data.getItems();
        for (byte i = 0; i < issueNames.size(); i++)
        {
            IssueName issueName = issueNames.get(i);
            issueName.setOrder(i);
        }
    }
}