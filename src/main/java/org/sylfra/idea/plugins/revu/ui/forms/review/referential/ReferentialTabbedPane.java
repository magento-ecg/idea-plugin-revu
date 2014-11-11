package org.sylfra.idea.plugins.revu.ui.forms.review.referential;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sylfra.idea.plugins.revu.RevuBundle;
import org.sylfra.idea.plugins.revu.model.*;
import org.sylfra.idea.plugins.revu.ui.forms.AbstractUpdatableForm;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.priority.IssuePriorityReferentialForm;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.tag.IssueTagReferentialForm;
import org.sylfra.idea.plugins.revu.ui.forms.review.referential.user.UserReferentialForm;
import org.sylfra.idea.plugins.revu.utils.RevuUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class ReferentialTabbedPane extends AbstractUpdatableForm<DataReferential>
{
  private final Project project;
  private JPanel contentPane;
  private JTabbedPane tabbedPane;
  private UserReferentialForm userReferentialForm;
  private IssueTagReferentialForm issueTagReferentialForm;
  private IssuePriorityReferentialForm issuePriorityReferentialForm;
  private JLabel lbNoUserForEmbeddedReviews;

  public ReferentialTabbedPane(final Project project)
  {
    this.project = project;

    userReferentialForm = new UserReferentialForm(project);
    issueTagReferentialForm = new IssueTagReferentialForm(project);
    issuePriorityReferentialForm = new IssuePriorityReferentialForm(project);

    lbNoUserForEmbeddedReviews = new JLabel(
      RevuBundle.message("projectSettings.review.referential.noUserForEmbeddedReviews.text"));
    lbNoUserForEmbeddedReviews.setHorizontalAlignment(SwingConstants.CENTER);
    JPanel pnUsers = new JPanel(new CardLayout());
    pnUsers.add("table", userReferentialForm.getContentPane());
    pnUsers.add("label", lbNoUserForEmbeddedReviews);

    tabbedPane.add(RevuBundle.message("projectSettings.review.referential.user.title"), pnUsers);
    tabbedPane.add(RevuBundle.message("projectSettings.review.referential.issueTag.title"),
      issueTagReferentialForm.getContentPane());
    tabbedPane.add(RevuBundle.message("projectSettings.review.referential.issuePriority.title"),
      issuePriorityReferentialForm.getContentPane());
  }


  public boolean isModified(@NotNull DataReferential data)
  {
    return ((!data.getReview().isEmbedded())
      && ((userReferentialForm.isModified(new ReferentialListHolder<User>(data.getUsers(true), null)))
      || (issueTagReferentialForm.isModified(new ReferentialListHolder<IssueTag>(data.getIssueTags(true), null)))
      || (issuePriorityReferentialForm.isModified(new ReferentialListHolder<IssuePriority>(
      data.getIssuePriorities(true), null)))));
  }

  @Override
  protected void internalUpdateWriteAccess(DataReferential data, @Nullable User user)
  {
  }

  protected void internalValidateInput(@Nullable DataReferential data)
  {
    updateError(userReferentialForm.getContentPane(), (data != null)
      && !userReferentialForm.validateInput(new ReferentialListHolder<User>(data.getUsers(true), null)), null);
    updateError(issueTagReferentialForm.getContentPane(),(data != null)
      && !issueTagReferentialForm.validateInput(new ReferentialListHolder<IssueTag>(data.getIssueTags(true), null)), null);
    updateError(issuePriorityReferentialForm.getContentPane(),(data != null)
      && !issuePriorityReferentialForm.validateInput(new ReferentialListHolder<IssuePriority>(data.getIssuePriorities(true), null)), null);

    updateTabIcons(tabbedPane);
  }

  protected void internalUpdateUI(DataReferential data, boolean requestFocus)
  {
    updateTabIcons(tabbedPane);

    Review review = getEnclosingReview(data);

    JPanel pnUsers = (JPanel) tabbedPane.getComponentAt(0);
    CardLayout userLayout = (CardLayout) pnUsers.getLayout();
    if ((data != null) && (data.getReview().isEmbedded()))
    {
      userLayout.show(pnUsers, "label");
    }
    else
    {
      userLayout.show(pnUsers, "table");
      ReferentialListHolder<User> userHolder = buildUsersListHolder(data);
      userReferentialForm.updateUI(review, userHolder, requestFocus);
    }

    ReferentialListHolder<IssueTag> tagHolder = buildItemTagsListHolder(data);
    issueTagReferentialForm.updateUI(review, tagHolder, requestFocus);

    ReferentialListHolder<IssuePriority> priorityHolder = buildItemPrioritiesListHolder(data);
    issuePriorityReferentialForm.updateUI(review, priorityHolder, requestFocus);
  }

  protected void internalUpdateData(@NotNull DataReferential data)
  {
    ReferentialListHolder<User> userHolder = new ReferentialListHolder<User>(data.getUsers(false),
      (data.getReview().getExtendedReview() == null)
        ? null : data.getReview().getExtendedReview().getDataReferential().getUsers(true));
    userReferentialForm.updateData(userHolder);
    data.setUsers(userHolder.getItems());

    ReferentialListHolder<IssueTag> tagHolder
      = new ReferentialListHolder<IssueTag>(data.getIssueTags(false),
      (data.getReview().getExtendedReview() == null)
        ? null : data.getReview().getExtendedReview().getDataReferential().getIssueTags(true));
    issueTagReferentialForm.updateData(tagHolder);
    data.setIssueTags(tagHolder.getItems());

    ReferentialListHolder<IssuePriority> priorityHolder
      = new ReferentialListHolder<IssuePriority>(data.getIssuePriorities(false),
      (data.getReview().getExtendedReview() == null)
        ? null : data.getReview().getExtendedReview().getDataReferential().getIssuePriorities(true));
    issuePriorityReferentialForm.updateData(priorityHolder);
    data.setIssuePriorities(priorityHolder.getItems());
  }

  private ReferentialListHolder<User> buildUsersListHolder(DataReferential data)
  {
    if (data == null)
    {
      return new ReferentialListHolder<User>(new ArrayList<User>(), null);
    }

    List<User> thisUsers = new ArrayList<User>(data.getUsers(false));
    Review extendedReview = data.getReview().getExtendedReview();
    if (extendedReview == null)
    {
      // List may be empty when extended review is removed
      if (thisUsers.isEmpty())
      {
        User user = RevuUtils.getCurrentUser();
        if (user != null)
        {
          for (User.Role role : User.Role.values())
          {
            user.addRole(role);
          }

          thisUsers.add(user);
        }
      }
      return new ReferentialListHolder<User>(thisUsers, null);
    }

    List<User> extendedUsers = new ArrayList<User>(extendedReview.getDataReferential().getUsers(true));
    for (ListIterator<User> it = extendedUsers.listIterator(); it.hasNext();)
    {
      User extendedUser = it.next();
      User user = data.getUser(extendedUser.getLogin(), false);
      if (user != null)
      {
        it.remove();

        // Use display name defined in extended review
        if (user.getDisplayName().equals(user.getLogin()))
        {
          user.setDisplayName(extendedUser.getDisplayName());
        }
      }
    }

    return new ReferentialListHolder<User>(thisUsers, extendedUsers);
  }

  private ReferentialListHolder<IssueTag> buildItemTagsListHolder(DataReferential data)
  {
    if (data == null)
    {
      return new ReferentialListHolder<IssueTag>(new ArrayList<IssueTag>(), null);
    }

    List<IssueTag> thisTags = data.getIssueTags(false);
    if (data.getReview().getExtendedReview() == null)
    {
      return new ReferentialListHolder<IssueTag>(thisTags, null);
    }

    List<IssueTag> extendedTags = new ArrayList<IssueTag>(
      data.getReview().getExtendedReview().getDataReferential().getIssueTags(true));
    extendedTags.removeAll(thisTags);

    return new ReferentialListHolder<IssueTag>(thisTags, extendedTags);
  }

  private ReferentialListHolder<IssuePriority> buildItemPrioritiesListHolder(DataReferential data)
  {
    if (data == null)
    {
      return new ReferentialListHolder<IssuePriority>(new ArrayList<IssuePriority>(), null);
    }

    List<IssuePriority> thisPriorities = data.getIssuePriorities(false);
    if (data.getReview().getExtendedReview() == null)
    {
      return new ReferentialListHolder<IssuePriority>(thisPriorities,
        null);
    }

    List<IssuePriority> extendedPriorities = new ArrayList<IssuePriority>(
      data.getReview().getExtendedReview().getDataReferential().getIssuePriorities(true));
    extendedPriorities.removeAll(thisPriorities);

    return new ReferentialListHolder<IssuePriority>(thisPriorities, extendedPriorities);
  }

  @Override
  public void dispose()
  {
    userReferentialForm.dispose();
    issueTagReferentialForm.dispose();
    issuePriorityReferentialForm.dispose();
  }

  public JComponent getPreferredFocusedComponent()
  {
    // @TODO
    return contentPane;
  }

  @NotNull
  public JPanel getContentPane()
  {
    return contentPane;
  }
}
