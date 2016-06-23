package gov.sparrow.fragment;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.contracts.NotebookContract;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.repository.NotebookRepository;
import gov.sparrow.util.TestActivity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowDialog;

import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class EditNotebookDialogFragmentTest {

    @Inject NotebookRepository notebookRepository;
    @Mock SparrowProvider contentProvider;
    @Captor ArgumentCaptor<NotebookRepository.DeleteNotebookListener> deleteCaptor;
    private EditNotebookDialogFragment subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProvider);

        subject = EditNotebookDialogFragment.newInstance(12L, "a title this is");
        startFragment(subject, TestActivity.class);

    }

    @Test
    public void editNotebookTitleField_shouldContainCurrentTitle() throws Exception {
        assertThat(subject.notebookTitle.getText().toString()).isEqualTo("a title this is");
    }

    @Test
    public void onNoteBookTitleField_shouldDisableSaveButton_whenTextRemoved() {
        subject.notebookTitle.setText("firstText");
        assertThat(subject.saveButton.isEnabled()).isTrue();

        subject.notebookTitle.setText("");
        assertThat(subject.saveButton.isEnabled()).isFalse();
    }

    @Test
    public void onSaveButtonPressedWithText_shouldUpdateNotebook() throws Exception {
        subject.notebookTitle.setText("TEST NOTEBOOK");
        subject.saveButton.performClick();

        verify(notebookRepository).updateNotebook(eq(12L), eq("TEST NOTEBOOK"), any(NotebookRepository.UpdateNotebookListener.class));
    }

    @Test
    public void onDeleteButtonPressed_shouldArchiveNotebook_andSetNotebookListSelection() throws Exception {
        subject.deleteButton.performClick();

        AlertDialog alertDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        verify(notebookRepository).archiveNotebook(eq(12L), eq(true), deleteCaptor.capture());

        deleteCaptor.getValue().onDeleteNotebookComplete(1);

        assertThat(TestActivity.NOTEBOOK_DELETE_CALLED).isTrue();
        assertThat(TestActivity.NOTEBOOK_LIST_SELECTION).isEqualTo(NotebookContract.Notebook.UNASSIGNED_NOTEBOOK_ID);
    }

    @Test
    public void onDeleteButtonPressed_shouldArchiveNotebook_andSetNotebookListSelection_andMoveAllNotesToAll() throws Exception {
        subject.deleteButton.performClick();

        AlertDialog alertDialog = (AlertDialog) ShadowDialog.getLatestDialog();
        alertDialog.getListView().setItemChecked(1, true);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        verify(notebookRepository).archiveNotebook(eq(12L), eq(false), deleteCaptor.capture());

        deleteCaptor.getValue().onDeleteNotebookComplete(1);

        assertThat(TestActivity.NOTEBOOK_DELETE_CALLED).isTrue();
        assertThat(TestActivity.NOTEBOOK_LIST_SELECTION).isEqualTo(NotebookContract.Notebook.UNASSIGNED_NOTEBOOK_ID);
    }

}