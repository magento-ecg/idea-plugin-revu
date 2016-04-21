package org.sylfra.idea.plugins.revu.actions.issue;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.sylfra.idea.plugins.revu.business.ReviewManager;
import org.sylfra.idea.plugins.revu.model.Issue;
import org.sylfra.idea.plugins.revu.model.IssueStatus;
import org.sylfra.idea.plugins.revu.model.Review;
import org.sylfra.idea.plugins.revu.model.User;
import org.sylfra.idea.plugins.revu.ui.forms.issue.IssueDialog;
import org.sylfra.idea.plugins.revu.utils.RevuUtils;
import org.sylfra.idea.plugins.revu.utils.RevuVcsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:syllant@gmail.com">Sylvain FRANCOIS</a>
 * @version $Id$
 */
public class CreateIssueAction extends AbstractIssueAction {

    protected String[] _targetPsiElementTypes = {"Class", "Class method"};


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(PlatformDataKeys.PROJECT);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        VirtualFile vFile = RevuUtils.getVirtualFile(e);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        if ((project == null) || (vFile == null)) {
            return;
        }

        Issue issue = new Issue();
        issue.setFile(vFile);
        if (editor != null) {
            Document document = editor.getDocument();
            int lineStart = document.getLineNumber(editor.getSelectionModel().getSelectionStart());
            int lineEnd = document.getLineNumber(editor.getSelectionModel().getSelectionEnd());

            Collection<PsiNamedElement> phpClassImpls = PsiTreeUtil.collectElementsOfType(psiFile, PsiNamedElement.class);
            Map<String, ArrayList<String>> foundPsiElements = getPsiElementsByTypes(e, this._targetPsiElementTypes);

            issue.setLineStart(lineStart);
            issue.setLineEnd(lineEnd);
            CharSequence fragment = document.getCharsSequence().subSequence(document.getLineStartOffset(lineStart),
                    document.getLineEndOffset(lineEnd));
            issue.setHash(fragment.toString().hashCode());
            issue.setSnippet(fragment.toString());
            issue.setClassName(foundPsiElements.get("Class").toString().replace("[", "").replace("]", ""));
            issue.setMethodName(foundPsiElements.get("Class method").toString().replace("[", "").replace("]", ""));

        }
        issue.setStatus(IssueStatus.TO_RESOLVE);

        IssueDialog dialog = new IssueDialog(project, true);
        dialog.show(issue);
        if ((dialog.isOK()) && dialog.updateData(issue)) {
            Review review = issue.getReview();

            assert (RevuUtils.getCurrentUserLogin() != null) : "Login should be set";

            issue.setHistory(RevuUtils.buildHistory(review));

            if (RevuVcsUtils.fileIsModifiedFromVcs(project, vFile)) {
                issue.setLocalRev(String.valueOf(System.currentTimeMillis()));
            }

            if (issue.getFile() != null) {
                VcsRevisionNumber vcsRev = RevuVcsUtils.getVcsRevisionNumber(project, issue.getFile());
                if (vcsRev != null) {
                    issue.setVcsRev(vcsRev.toString());
                }
            }

            review.addIssue(issue);

            ReviewManager reviewManager = project.getComponent(ReviewManager.class);
            reviewManager.saveSilently(review);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = false;
        Project project = e.getData(PlatformDataKeys.PROJECT);
        VirtualFile vFile = RevuUtils.getVirtualFile(e);

        if ((project != null) && (vFile != null)) {
            Collection<Review> reviews = RevuUtils.getActiveReviewsForCurrentUser(project);
            for (Review review : reviews) {
                User user = RevuUtils.getCurrentUser(review);
                if (user == null) {
                    continue;
                }

                boolean mayReview = RevuUtils.isActive(review) && user.hasRole(User.Role.REVIEWER);
                if ((mayReview || (user.hasRole(User.Role.ADMIN)))) {
                    enabled = true;
                    break;
                }
            }
        }

        e.getPresentation().setEnabled(enabled);
    }

    protected Map<String, ArrayList<String>> getPsiElementsByTypes(AnActionEvent e, String[] inputTypes) {
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        Document document = editor.getDocument();

        Map<String, ArrayList<String>> foundItems = new HashMap<String, ArrayList<String>>();
        for (String itemName : inputTypes) {
            foundItems.put(itemName, new ArrayList<String>());
        }

        Collection<PsiNamedElement> foundPsiElements = PsiTreeUtil.collectElementsOfType(psiFile, PsiNamedElement.class);
        int selectionStart = document.getLineNumber(editor.getSelectionModel().getSelectionStart());
        int selectionEnd = document.getLineNumber(editor.getSelectionModel().getSelectionEnd());

        for (PsiNamedElement k : foundPsiElements) {
            int _LineStart = document.getLineNumber(k.getTextRange().getStartOffset());
            int _LineEnd = document.getLineNumber(k.getTextRange().getEndOffset());

            if (!((_LineStart > selectionEnd) || (_LineEnd < selectionStart))) {
                for (String itemName : inputTypes) {
                    if (itemName.equals(k.getNode().getElementType().toString())) {
                        foundItems.get(itemName).add(k.getName());
                    }
                }
            }
        }

        return foundItems;
    }
}
