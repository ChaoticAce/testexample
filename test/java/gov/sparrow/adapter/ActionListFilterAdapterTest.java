package gov.sparrow.adapter;

import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NotebookContract.Notebook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SparrowTestRunner.class)
public class ActionListFilterAdapterTest {

    private ActionListFilterAdapter subject;

    @Before
    public void setUp() throws Exception {
        subject = new ActionListFilterAdapter(RuntimeEnvironment.application);
    }

    @Test
    public void bindView_whenAllNotebook_shouldBindAllActionsView() throws Exception {
        MatrixCursor cursor = setUpCursor(Notebook.UNASSIGNED_NOTEBOOK_ID);

        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.action_filter_item, null, false);
        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat(((TextView) view.findViewById(R.id.filter_item_title)).getText().toString()).isEqualTo("All Actions");
        Drawable[] drawables = ((TextView) view.findViewById(R.id.filter_item_title)).getCompoundDrawables();
        Drawable drawable = drawables[0];
        assertThat(shadowOf(drawable).getCreatedFromResId()).isEqualTo(R.drawable.ic_action);
    }

    @Test
    public void bindView_whenNotebook_shouldBindNotebookView() throws Exception {
        MatrixCursor cursor = setUpCursor(1L);

        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.action_filter_item, null, false);
        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat(((TextView) view.findViewById(R.id.filter_item_title)).getText().toString()).isEqualTo("test title");
        Drawable[] drawables = ((TextView) view.findViewById(R.id.filter_item_title)).getCompoundDrawables();
        Drawable drawable = drawables[0];
        assertThat(shadowOf(drawable).getCreatedFromResId()).isEqualTo(R.drawable.ic_notebook_header);
    }

    private MatrixCursor setUpCursor(long id) {
        MatrixCursor cursor = new MatrixCursor(new String[]{Notebook._ID, Notebook.COLUMN_NAME_TITLE});
        cursor.addRow(new Object[]{id, "test title"});
        cursor.moveToFirst();
        return cursor;
    }

}