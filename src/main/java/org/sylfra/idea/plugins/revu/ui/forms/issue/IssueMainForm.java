package org.sylfra.idea.plugins.revu.ui.forms.issue;

import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.sylfra.idea.plugins.revu.RevuBundle;
import org.sylfra.idea.plugins.revu.RevuIconProvider;
import org.sylfra.idea.plugins.revu.business.IReviewListener;
import org.sylfra.idea.plugins.revu.business.ReviewManager;
import org.sylfra.idea.plugins.revu.model.*;
import org.sylfra.idea.plugins.revu.ui.editor.RevuEditorHandler;
import org.sylfra.idea.plugins.revu.ui.multichooser.MultiChooserPanel;
import org.sylfra.idea.plugins.revu.ui.multichooser.UniqueNameMultiChooserItem;
import org.sylfra.idea.plugins.revu.utils.RevuUtils;
import org.sylfra.idea.plugins.revu.utils.RevuVfsUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class IssueMainForm extends AbstractIssueForm
{
    private final boolean createMode;
    private final boolean inDialog;
    private MultiChooserPanel<IssueTag, UniqueNameMultiChooserItem<IssueTag>> tagsMultiChooserPanel;
    private JPanel contentPane;
    private JTextArea taDesc;
    private ComboBox cbPriority;
    private JTextArea taSummary;
    private JTextField tfLocation;
    private ComboBox cbReview;
    private JRadioButton rbLocationFile;
    private JRadioButton rbLocationGlobal;
    private JRadioButton rbLocationLineRange;
    private JLabel lbSync;
    private JLabel lbTags;
    private JLabel lbReview;
    private JPanel pnReview;
    private JTextField tfClassName;
    private JTextField tfMethodName;
    private JBList cbIssueName;
    private JButton copyFieldsButton;
    private JButton pasteFieldsButton;
    private ButtonGroup bgLocation;

    private static Map<String, String> cachedFields;
    private static List<IssueTag> cachedTags;
    private static IssueName cachedIssueName;

    public IssueMainForm(@NotNull Project project, boolean createMode, boolean inDialog)
    {
        super(project);
        this.createMode = createMode;
        this.inDialog = inDialog;
        configureUI();
        copyFieldsButton.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseReleased(MouseEvent e) {
              super.mouseReleased(e);
              final IssuePriority priority = (IssuePriority) cbPriority.getSelectedItem();
              cachedFields = new HashMap<String, String>() {{
                  put("Description", taSummary.getText());
                  put("Recommendation", taDesc.getText());
                  put("Priority", priority.getName());
              }};
              cachedTags = tagsMultiChooserPanel.getSelectedItemDatas();
              cachedIssueName = (IssueName) cbIssueName.getSelectedValue();
            }
      });
        pasteFieldsButton.addMouseListener(new MouseAdapter() {
          @Override
          public void mouseReleased(MouseEvent e) {
              super.mouseReleased(e);
              if (cachedFields != null) {
                  insertFields(
                          cachedFields.get("Description"),
                          cachedFields.get("Recommendation"),
                          cachedFields.get("Priority"),
                          cachedTags,
                          cachedIssueName
                  );
              }
            }
      });
    }

  private void createUIComponents()
  {
    lbTags = new JLabel();
    tagsMultiChooserPanel = new MultiChooserPanel<IssueTag, UniqueNameMultiChooserItem<IssueTag>>(project, lbTags,
      RevuBundle.message("issueForm.tagsPopup.title"),
      "TagsChooser" + (inDialog ? "Dialog" : ""), RevuIconProvider.IconRef.TAG)
    {
      protected UniqueNameMultiChooserItem<IssueTag> createMultiChooserItem(@NotNull IssueTag issueTag)
      {
        return new UniqueNameMultiChooserItem<IssueTag>(issueTag);
      }

      @Override
      protected List<IssueTag> retrieveAllAvailableElements()
      {
        Review review = (createMode) ? (Review) cbReview.getSelectedItem() : currentIssue.getReview();

        return review.getDataReferential().getIssueTags(true);
      }
    };
  }

  private void configureUI()
  {
    RevuUtils.configureTextAreaAsStandardField(taDesc, taSummary);

    tfLocation.setBorder(BorderFactory.createEmptyBorder());

    ((CardLayout) pnReview.getLayout()).show(pnReview, createMode ? "combo" : "label");

    if (createMode)
    {
      cbReview.setModel(new ReviewComboBoxModel(project));
      //  cbReview.setRenderer(new ListCellRendererWrapper<Review>(cbReview)
        cbReview.setRenderer(new ListCellRendererWrapper<Review>()
      {
        @Override
        public void customize(JList list, Review review, int index, boolean selected, boolean hasFocus)
        {
          String tooltip;
          if (review == null)
          {
            tooltip = null;
            setText(RevuBundle.message("general.selectComboValue.text"));
          }
          else
          {
            tooltip = review.isExternalizable() ? review.getFile().getPath() : review.getName();
            setText(review.getName());
          }

          if (tooltip != null)
          {
            setToolTipText(tooltip);
          }
       }
      });

      cbReview.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          Object selectedReview = cbReview.getSelectedItem();
          if (selectedReview != null)
          {
            DataReferential referential = ((Review) selectedReview).getDataReferential();

            cbPriority.setModel(new DefaultComboBoxModel(buildComboItemsArray(
              new TreeSet<IssuePriority>(referential.getIssuePrioritiesByName(true).values()), true)));
            tagsMultiChooserPanel.setEnabled(true);
              cbIssueName.setModel(new DefaultComboBoxModel(buildComboItemsArray(
                      new TreeSet<IssueName>(referential.getIssueNamesByName(true).values()), true)));

            tagsMultiChooserPanel.setEnabled(true);
          }
          else
          {
//             "[Select a value]" String is selected
            cbPriority.setModel(new DefaultComboBoxModel(buildComboItemsArray(new ArrayList(0), true)));
            cbIssueName.setModel(new DefaultComboBoxModel(buildComboItemsArray(new ArrayList(0), true)));


            tagsMultiChooserPanel.setEnabled(false);
          }
        }
      });
    }

    cbPriority.setRenderer(new DefaultListCellRenderer()
    {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
      {
        if (value == null)
        {
          value = RevuBundle.message(((createMode) && (cbReview.getSelectedItem() == null))
            ? "general.selectReviewBeforeFillingCombo.text" : "general.selectComboValue.text");
        }
        else
        {
          value = ((IssuePriority) value).getName();
        }
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }
    });

      new ListSpeedSearch(cbIssueName) {
          @Override
          protected boolean compare(final String s, final String s1) {
              return s != null && s1 != null ? s.toUpperCase().contains(s1.toUpperCase()) : super.compare(s, s1);
          }
          @Override
          protected void selectElement(Object element, String selectedText) {
              super.selectElement(element,selectedText);
              Object selectedIssue = cbIssueName.getSelectedValue();
              getFieldsForInsert(selectedIssue);
          }
      };

      cbIssueName.addListSelectionListener(new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent e) {
              Object selectedIssue = cbIssueName.getSelectedValue();
              if (selectedIssue != null && e.getValueIsAdjusting()) {
                  getFieldsForInsert(selectedIssue);
              }
          }
      });

    ActionListener locationTypeListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        Issue.LocationType locationType = (rbLocationGlobal.isSelected())
          ? Issue.LocationType.GLOBAL
          : ((rbLocationFile.isSelected()) ? Issue.LocationType.FILE : Issue.LocationType.LINE_RANGE);
        updateLocation(locationType);
      }
    };
    rbLocationGlobal.addActionListener(locationTypeListener);
    rbLocationFile.addActionListener(locationTypeListener);
    rbLocationLineRange.addActionListener(locationTypeListener);

    lbSync.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        assert currentIssue != null;

        RevuEditorHandler editorHandler = project.getComponent(RevuEditorHandler.class);
        currentIssue.setHash(editorHandler.buildNewHash(currentIssue));
        currentIssue.getReview().fireIssueUpdated(currentIssue);
      }
    });
  }

    public void getFieldsForInsert(Object selectedIssue)
    {
        String description = ((IssueName) selectedIssue).getDescription();
        String recommendation = ((IssueName) selectedIssue).getRecommendation();
        String priority = ((IssueName) selectedIssue).getPriority();
        String tags = ((IssueName) selectedIssue).getTags();
        List<IssueTag> tList = new ArrayList<IssueTag>();
        List<String> sList = new ArrayList<String>(Arrays.asList(tags.split(",")));
        for (String t : sList) {
            if (!t.isEmpty()) {
                tList.add(new IssueTag(t));
            }
        }
        insertFields(description, recommendation, priority, tList, null);
    }

   private void insertFields(String description, String recommendation, String priority, List<IssueTag> tags, IssueName issueName)
   {
       if (description != null) {
           taSummary.setText(description);
       }
       if (recommendation != null) {
           taDesc.setText(recommendation);
       }
       if (recommendation != null) {
           taDesc.setText(recommendation);
       }
       if (priority != null) {
           byte order = -1;
           //todo: remove hardcode
           if (priority.equals("High")) {
               order = IssuePriority.PRIORITY_HIGH_ORDER;
           } else if (priority.equals("Medium")) {
               order = IssuePriority.PRIORITY_MEDIUM_ORDER;
           } else if (priority.equals("Low")) {
               order = IssuePriority.PRIORITY_LOW_ORDER;
           }
           cbPriority.setSelectedItem(new IssuePriority(order, priority));
       }
       if (!tags.isEmpty()) {
           tagsMultiChooserPanel.setSelectedItemDatas(tags);
       }
       if (issueName != null) {
           cbIssueName.setSelectedValue(issueName, true);
       }
   }

  public JComponent getPreferredFocusedComponent()
  {
    return cbReview.getSelectedIndex() == 0 ? cbReview : cbIssueName;
  }

  @NotNull
  public JPanel getContentPane()
  {
    return contentPane;
  }

  protected void internalUpdateUI(@Nullable Issue data, boolean requestFocus)
  {
    super.internalUpdateUI(data, requestFocus);

    if (createMode)
    {
      ReviewComboBoxModel reviewComboBoxModel = (ReviewComboBoxModel) cbReview.getModel();
      reviewComboBoxModel.updateReviews();

      Review defaultReview;
      if ((data == null) || (data.getReview() == null))
      {
        defaultReview = RevuUtils.getReviewingReview(project);

        // No reviewing review, see if there is only one review
        if (defaultReview == null)
        {
          if (reviewComboBoxModel.getSize() == 2)
          {
            defaultReview = (Review) reviewComboBoxModel.getElementAt(1);
          }
        }
      }
      else
      {
        defaultReview = data.getReview();
      }

      cbReview.setSelectedItem(defaultReview);
    }
    else
    {
      if ((data != null) && (data.getReview() != null))
      {
        cbPriority.setModel(new DefaultComboBoxModel(buildComboItemsArray(
          new TreeSet<IssuePriority>(
            currentIssue.getReview().getDataReferential().getIssuePrioritiesByName(true).values()), true)));
          cbIssueName.setModel(new DefaultComboBoxModel(buildComboItemsArray(
                  new TreeSet<IssueName>(
                          currentIssue.getReview().getDataReferential().getIssueNamesByName(true).values()), true)));

        lbReview.setText(RevuBundle.message("issueForm.main.review.text", data.getReview().getName(),
          RevuUtils.buildReviewStatusLabel(data.getReview().getStatus(), true)));
      }
    }

    taDesc.setText((data == null) ? "" : data.getDesc());
    taSummary.setText((data == null) ? "" : data.getSummary());
    cbPriority.setSelectedItem((data == null) ? null : data.getPriority());
      cbIssueName.setSelectedValue((data == null) ? null : data.getIssueName(), true);
      tfClassName.setText((data == null) ? "" : data.getClassName());
      tfMethodName.setText((data == null) ? "" : data.getMethodName());
    lbSync.setVisible((data != null) && (!isIssueSynchronized(data)));
    tagsMultiChooserPanel.setSelectedItemDatas((data == null) ? null : data.getTags());
    tagsMultiChooserPanel.setEnabled((!createMode) || (cbReview.getSelectedItem() != null));

    Issue.LocationType locationType = (data == null) ? null : data.getLocationType();
    updateLocation(locationType);
  }

  protected void internalUpdateData(@NotNull Issue data)
  {
    if (createMode)
    {
      data.setReview((Review) cbReview.getSelectedItem());
    }

    data.setDesc(taDesc.getText());
    data.setSummary(taSummary.getText());
    data.setClassName(tfClassName.getText());
    data.setMethodName(tfMethodName.getText());
    data.setIssueName((IssueName) cbIssueName.getSelectedValue());
    data.setPriority((IssuePriority) cbPriority.getSelectedItem());
    data.setTags(tagsMultiChooserPanel.getSelectedItemDatas());

    // Location
    if (rbLocationGlobal.isSelected())
    {
      data.setFile(null);
      data.setLineStart(-1);
      data.setLineEnd(-1);
    }
    else if (rbLocationFile.isSelected())
    {
      data.setLineStart(-1);
      data.setLineEnd(-1);
    }
  }

  public boolean isModified(@NotNull Issue data)
  {
    if (!checkEquals(taDesc.getText(), data.getDesc()))
    {
      return true;
    }

    if (!checkEquals(taSummary.getText(), data.getSummary()))
    {
      return true;
    }

      if (!checkEquals(tfClassName.getText(), data.getClassName())) {
          return true;
      }

      if (!checkEquals(tfMethodName.getText(), data.getMethodName())) {
          return true;
      }
      if (!checkEquals(cbIssueName.getSelectedValue(), data.getIssueName())) {
          return true;
      }



      if ((createMode) && (!checkEquals(cbReview.getSelectedItem(), data.getReview())))
    {
      return true;
    }

    if (!checkEquals(cbPriority.getSelectedItem(), data.getPriority()))
    {
      return true;
    }

    if (!checkEquals(tagsMultiChooserPanel.getSelectedItemDatas(), data.getTags()))
    {
      return true;
    }

    if ((bgLocation.getSelection() != null)
      && (!checkEquals(bgLocation.getSelection().getActionCommand(), data.getLocationType().toString())))
    {
      return true;
    }

    return false;
  }

  @Override
  protected void internalUpdateWriteAccess(Issue data, @Nullable User user)
  {
    boolean mayReview = (((createMode) || (user != null))
      && ((currentIssue == null) || (currentIssue.getReview() == null)
      || (IssueStatus.CLOSED != currentIssue.getStatus())));
    RevuUtils.setWriteAccess(mayReview, cbPriority,
        taSummary, taDesc, rbLocationGlobal, tfClassName, tfMethodName, cbIssueName);

    Issue.LocationType locationType = (currentIssue == null) ? null : currentIssue.getLocationType();
    rbLocationFile.setEnabled(mayReview && !Issue.LocationType.GLOBAL.equals(locationType));
    rbLocationLineRange.setEnabled(mayReview && !Issue.LocationType.GLOBAL.equals(locationType)
      && !Issue.LocationType.FILE.equals(locationType));

    tagsMultiChooserPanel.setEnabled(mayReview && ((!createMode) || (cbReview.getSelectedIndex() > 0)));
  }

  public void internalValidateInput(@Nullable Issue data)
  {
    updateRequiredError(taSummary, "".equals(taSummary.getText().trim()));
    updateRequiredError(cbReview, (createMode && (!(cbReview.getSelectedItem() instanceof Review))));
  }

  private boolean isIssueSynchronized(Issue issue)
  {
    RevuEditorHandler editorHandler = project.getComponent(RevuEditorHandler.class);
    return editorHandler.isSynchronized(issue, true);
  }

  private void updateLocation(@Nullable Issue.LocationType locationType)
  {
    if (locationType == null)
    {
      rbLocationGlobal.setSelected(true);
      tfLocation.setText("");
    }
    else
    {
      String filePath;
      String locationPath;
      switch (locationType)
      {
        case GLOBAL:
          rbLocationGlobal.setSelected(true);
          locationPath = RevuBundle.message("issueForm.main.location.globalPath.text");
          break;

        case FILE:
          rbLocationFile.setSelected(true);
          filePath = RevuVfsUtils.buildRelativePath(project, currentIssue.getFile());
          locationPath = (currentIssue.getVcsRev() == null)
            ? filePath
            : RevuBundle.message("issueForm.main.location.pathWithVcsRev.text", filePath,
            currentIssue.getVcsRev());
          break;

        default:
          rbLocationLineRange.setSelected(true);
          filePath = RevuVfsUtils.buildRelativePath(project, currentIssue.getFile());
          String filePathWithVcsRev = (currentIssue.getVcsRev() == null)
            ? filePath
            : RevuBundle.message("issueForm.main.location.pathWithVcsRev.text", filePath,
            currentIssue.getVcsRev());
          locationPath = RevuBundle.message("issueForm.main.location.range.path.text",
            filePathWithVcsRev, (currentIssue.getLineStart() + 1), (currentIssue.getLineEnd() + 1));
      }

      tfLocation.setText(locationPath);
      tfLocation.setToolTipText(locationPath);
    }
  }

  private static class ReviewComboBoxModel extends AbstractListModel implements IReviewListener, ComboBoxModel
  {
    private final static Comparator<Review> REVIEW_COMPARATOR = new Comparator<Review>()
    {
      /**
       * {@inheritDoc}
       */
      public int compare(Review r1, Review r2)
      {
        return (r1 == null) ? -1 : ((r2 == null) ? 1 : r1.getName().compareTo(r2.getName()));
      }
    };

    private final Project project;
    private final List<Review> reviews;
    private Review selectedReview;

    private ReviewComboBoxModel(@NotNull Project project)
    {
      this.project = project;
      reviews = new ArrayList<Review>(1);
      reviews.add(null);

      ReviewManager reviewManager = project.getComponent(ReviewManager.class);
      reviewManager.addReviewListener(this);
    }

    public int getSize()
    {
      return reviews.size();
    }

    public Object getElementAt(int index)
    {
      return reviews.get(index);
    }

    public void setSelectedItem(Object item)
    {
      if (((selectedReview != null) && !selectedReview.equals(item)) || ((selectedReview == null) && (item != null)))
      {
        selectedReview = (Review) item;
        fireContentsChanged(this, -1, -1);
      }
    }

    public Object getSelectedItem()
    {
      return selectedReview;
    }

    public void updateReviews()
    {
      reviews.clear();
      reviews.add(null);
      reviews.addAll(RevuUtils.getActiveReviewsForCurrentUser(project));
      Collections.sort(reviews, REVIEW_COMPARATOR);
    }

    public void reviewAdded(Review review)
    {
      if (!RevuUtils.isActiveForCurrentUser(review))
      {
        return;
      }

      reviews.add(review);
      Collections.sort(reviews, REVIEW_COMPARATOR);

      int index = reviews.indexOf(review);
      fireIntervalAdded(this, index, index);
    }

    public void reviewChanged(Review review)
    {
      int index = reviews.indexOf(review);
      if (index == -1)
      {
        if (RevuUtils.isActiveForCurrentUser(review))
        {
          reviewAdded(review);
        }
      }
      else
      {
        if (!RevuUtils.isActiveForCurrentUser(review))
        {
          reviewDeleted(review);
        }
        else
        {
          fireContentsChanged(this, index, index);
        }
      }
    }

    public void reviewDeleted(Review review)
    {
      int index = reviews.indexOf(review);
      if (index > -1)
      {
        reviews.remove(review);
        fireIntervalRemoved(this, index, index);
      }
    }
  }
}
