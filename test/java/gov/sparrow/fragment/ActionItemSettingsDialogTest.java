package gov.sparrow.fragment;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.repository.ActionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class ActionItemSettingsDialogTest {

    @Inject ActionRepository actionRepository;
    private ActionItemSettingsDialog subject;

    @Before
    public void setUp() {
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        subject = ActionItemSettingsDialog.newInstance(1L, 22L, "test action body", "test note title", "1999-02-22 00:00:00.000");
        startFragment(subject);
    }

    @Test
    public void onCancelClicked_shouldDismiss() {
        assertThat(subject.getDialog()).isNotNull();
        subject.cancelButton.performClick();
        assertThat(subject.getDialog()).isNull();
    }

    @Test
    public void fragment_shouldHandleNullNoteId() {
        ActionItemSettingsDialog dialog = ActionItemSettingsDialog.newInstance(1L, null, "test action body", "test note title", "1999-02-22 00:00:00.000");
        startFragment(dialog);
    }

    @Test
    public void onDeleteClicked_shouldOpenDeleteConfirmationDialog() throws Exception {
        subject.deleteButton.performClick();
        verify(actionRepository).archiveAction(1L);
    }

    @Test
    public void onDeleteClicked_shouldDismiss() throws Exception {
        assertThat(subject.getDialog()).isNotNull();
        subject.deleteButton.performClick();
        assertThat(subject.getDialog()).isNull();
    }

}