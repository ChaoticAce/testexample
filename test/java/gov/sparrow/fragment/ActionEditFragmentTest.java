package gov.sparrow.fragment;

import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.models.Action;
import gov.sparrow.models.builders.ActionBuilder;
import gov.sparrow.repository.ActionRepository;
import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.shadows.support.v4.SupportFragmentTestUtil.startFragment;

@RunWith(SparrowTestRunner.class)
public class ActionEditFragmentTest {

    @Inject ActionRepository actionRepository;
    @Mock ActionRepository.UpdateActionListener listener;

    private ActionEditFragment subject;

    @Before
    public void setUp() {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        subject = ActionEditFragment.newInstance(1L, 22L, "test action body", "test note title");
        startFragment(subject);
    }

    @Test
    public void onCreateView_bindsViewFromBundle() {
        assertThat(subject.editActionBody.getText().toString()).isEqualTo("test action body");
        assertThat(subject.editActionNoteTitle.getText().toString()).isEqualTo("test note title");
    }

    @Test
    public void onCreateView_shouldHideNoteTitle_whenNoTitle() {
        subject = ActionEditFragment.newInstance(1L, 22L, "test action body", null);
        startFragment(subject);

        assertThat(subject.editActionBody.getText().toString()).isEqualTo("test action body");
        assertThat(subject.editActionNoteTitle.getText().toString()).isEqualTo("");
        ANDROID.assertThat(subject.editActionNoteTitle).isNotVisible();
    }

    @Test
    public void applyChanges_shouldSetBodyNoteIdAndId() {
        subject.editActionBody.setText("new action body");
        ActionBuilder actionBuilder = ActionBuilder.actionBuilder();
        subject.applyChanges(actionBuilder);

        Action action = actionBuilder.build();

        assertThat(action.getId()).isEqualTo(1L);
        assertThat(action.getNoteId()).isEqualTo(22L);
        assertThat(action.getTitle()).isEqualTo("new action body");
    }

    @Test
    public void fragment_shouldHandleNullNoteId() {
        ActionEditFragment fragment = ActionEditFragment.newInstance(1L, null, "test action body", "test note title");
        startFragment(fragment);
    }

}