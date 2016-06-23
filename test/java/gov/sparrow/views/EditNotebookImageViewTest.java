package gov.sparrow.views;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static gov.sparrow.contracts.NotebookContract.Notebook.UNASSIGNED_NOTEBOOK_ID;
import static org.fest.assertions.api.ANDROID.assertThat;


@RunWith(SparrowTestRunner.class)
public class EditNotebookImageViewTest {

    private EditNotebookImageView subject;

    @Before
    public void setUp() throws Exception {
        subject = new EditNotebookImageView(RuntimeEnvironment.application);
    }

    @Test
    public void activating_whenNotAllNotebook_shouldMakeViewVisible() throws Exception {
        subject.setVisibility(GONE);
        subject.setTag(R.id.notebook_id, "1");
        subject.setActivated(true);

        assertThat(subject).isVisible();
    }

    @Test
    public void activating_whenAllNotebook_shouldMakeViewGone() throws Exception {
        subject.setVisibility(GONE);
        subject.setTag(R.id.notebook_id, UNASSIGNED_NOTEBOOK_ID);
        subject.setActivated(true);

        assertThat(subject).isGone();
    }

    @Test
    public void deactivating_shouldMakeViewGone() throws Exception {
        subject.setVisibility(VISIBLE);
        subject.setActivated(false);

        assertThat(subject).isGone();
    }

    @Test
    public void activating_whenNoTagSet_shouldMakeViewVisible() throws Exception {
        subject.setVisibility(GONE);
        subject.setActivated(true);

        assertThat(subject).isVisible();
    }
}