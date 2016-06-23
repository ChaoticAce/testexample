package gov.sparrow.adapter;

import android.database.Cursor;
import android.database.MatrixCursor;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.contracts.NoteContract.Note;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static gov.sparrow.contracts.ActionContract.Action;
import static gov.sparrow.contracts.ActionContract.ActionListPosition;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class ActionListAdapterCursorWrapperTest {

    private ActionListAdapterCursorWrapper subject;
    private Cursor cursor;

    @Before
    public void setUp() throws Exception {
        cursor = setupWrappedCursor(new Object[][]{
                new Object[]{"true", 30, 0L},
                new Object[]{"false", 20, 1L},
                new Object[]{"true", 10, 2L}
        });

        subject = new ActionListAdapterCursorWrapper(cursor, null);
    }

    @Test
    public void getFirstCompletedPosition_whenCompletedItemsPresent_returnsFirstCompletedPosition() throws Exception {
        assertThat(subject.getFirstCompletedPosition()).isEqualTo(1);

        subject = new ActionListAdapterCursorWrapper(cursor, 1L);
        assertThat(subject.getFirstCompletedPosition()).isEqualTo(1);
    }

    @Test
    public void getFirstCompletedPosition_whenNoCompletedItemsPresent_returnsLastPosition() throws Exception {
        Cursor noCompletedItemsCursor = setupWrappedCursor(new Object[][]{
                new Object[]{"false", 30, 0L},
                new Object[]{"false", 20, 1L},
                new Object[]{"false", 10, 2L}
        });

        subject = new ActionListAdapterCursorWrapper(noCompletedItemsCursor, null);
        assertThat(subject.getFirstCompletedPosition()).isEqualTo(2);

        subject = new ActionListAdapterCursorWrapper(noCompletedItemsCursor, 1L);
        assertThat(subject.getFirstCompletedPosition()).isEqualTo(2);
    }

    @Test
    public void getLastIncompletePosition_whenIncompleteItemsPresent_returnsLastIncompletePosition() throws Exception {
        assertThat(subject.getLastIncompletePosition()).isEqualTo(0);

        subject = new ActionListAdapterCursorWrapper(cursor, 1L);
        assertThat(subject.getLastIncompletePosition()).isEqualTo(0);
    }

    @Test
    public void getLastIncompletePosition_whenNoIncompleteItemsPresent_returnsFirstPosition() throws Exception {
        Cursor noIncompleteItemsCursor = setupWrappedCursor(new Object[][]{
                new Object[]{"true", 30, 0L},
                new Object[]{"true", 20, 1L},
                new Object[]{"true", 10, 2L}
        });

        subject = new ActionListAdapterCursorWrapper(noIncompleteItemsCursor, null);
        assertThat(subject.getLastIncompletePosition()).isEqualTo(0);

        subject = new ActionListAdapterCursorWrapper(noIncompleteItemsCursor, 1L);
        assertThat(subject.getLastIncompletePosition()).isEqualTo(0);
    }

    @Test
    public void rewindsCursorBeforeSettingUpPositionList() throws Exception {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Action.COLUMN_NAME_COMPLETED,
                ActionListPosition.COLUMN_NAME_POSITION,
                Note.COLUMN_NAME_NOTEBOOK_ID
        });
        cursor.addRow(new Object[]{"true", 20, 1L});

        subject = new ActionListAdapterCursorWrapper(cursor, null);

        subject.getWrappedCursor().moveToPosition(0);

        assertThat(subject.getCount()).isEqualTo(1);
        assertThat(subject.getMappedPosition(0)).isEqualTo(20);
    }

    @Test
    public void setupPositionList_handlesGaps() throws Exception {
        cursor = setupWrappedCursor(new Object[][]{
                new Object[]{"true", 30, 0L},
                new Object[]{"false", 20, 1L},
                new Object[]{"true", 10, 2L}
        });

        subject = new ActionListAdapterCursorWrapper(cursor, null);

        subject.getWrappedCursor().moveToPosition(0);

        assertThat(subject.getCount()).isEqualTo(3);
        assertThat(subject.getMappedPosition(0)).isEqualTo(10);
        assertThat(subject.getMappedPosition(1)).isEqualTo(20);
        assertThat(subject.getMappedPosition(2)).isEqualTo(30);
    }

    @Test
    public void getCount_shouldReturnCountForFilter() throws Exception {
        cursor = setupWrappedCursor(new Object[][]{
                new Object[]{"true", 30, 0L},
                new Object[]{"false", 20, 1L},
                new Object[]{"true", 10, 2L},
                new Object[]{"true", 40, 2L}
        });

        subject = new ActionListAdapterCursorWrapper(cursor, 2L);

        assertThat(subject.getCount()).isEqualTo(2);
    }

    private Cursor setupWrappedCursor(Object[][] rows) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                Action.COLUMN_NAME_COMPLETED,
                ActionListPosition.COLUMN_NAME_POSITION,
                Note.COLUMN_NAME_NOTEBOOK_ID
        });

        for (Object[] row : rows) {
            cursor.addRow(row);
        }
        return cursor;
    }
}