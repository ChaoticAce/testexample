package gov.sparrow.adapter.helpers;

import android.database.Cursor;
import android.database.MatrixCursor;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.util.TestSortableCursorWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class SortableCursorWrapperTest {
    private SortableCursorWrapper subject;
    private Cursor cursor;

    @Before
    public void setUp() throws Exception {
        cursor = setupWrappedCursor(new Object[][]{
                new Object[]{22},
                new Object[]{10},
                new Object[]{31}
        });

        subject = new TestSortableCursorWrapper(cursor);
    }

    @Test
    public void moveToFirst_movesWrappedCursorToItemWithFirstPosition() throws Exception {
        subject.moveToFirst();

        assertThat(cursor.getPosition()).isEqualTo(1);
    }

    @Test
    public void moveToLast_movesWrappedCursorToItemWithLastPosition() throws Exception {
        subject.moveToLast();

        assertThat(cursor.getPosition()).isEqualTo(2);
    }

    @Test
    public void moveToPrevious_movesWrappedCursorToItemWithPreviousPosition() throws Exception {
        subject.moveToLast();

        subject.moveToPrevious();
        assertThat(cursor.getPosition()).isEqualTo(0);

        subject.moveToPrevious();
        assertThat(cursor.getPosition()).isEqualTo(1);
    }

    @Test
    public void moveToPrevious_whenOutOfBounds_returnsFalse() throws Exception {
        subject.moveToFirst();

        assertThat(subject.moveToPrevious()).isFalse();
    }

    @Test
    public void moveToPrevious_whenOutOfBounds_doesNotMoveCursorPosition() throws Exception {
        subject.moveToFirst();

        subject.moveToPrevious();

        assertThat(cursor.getPosition()).isEqualTo(1);
        assertThat(subject.getPosition()).isEqualTo(0);
    }

    @Test
    public void moveToPrevious_whenNotOutOfBounds_returnsTrue() throws Exception {
        subject.moveToLast();

        assertThat(subject.moveToPrevious()).isTrue();
        assertThat(subject.moveToPrevious()).isTrue();
    }

    @Test
    public void moveToNext_movesWrappedCursorToItemWithNextPosition() throws Exception {
        subject.moveToFirst();

        subject.moveToNext();
        assertThat(cursor.getPosition()).isEqualTo(0);

        subject.moveToNext();
        assertThat(cursor.getPosition()).isEqualTo(2);
    }

    @Test
    public void moveToNext_whenOutOfBounds_returnsFalse() throws Exception {
        subject.moveToLast();

        assertThat(subject.moveToNext()).isFalse();
    }

    @Test
    public void moveToNext_whenOutOfBounds_doesNotMoveCursorPosition() throws Exception {
        subject.moveToLast();

        subject.moveToNext();

        assertThat(cursor.getPosition()).isEqualTo(2);
        assertThat(subject.getPosition()).isEqualTo(2);
    }

    @Test
    public void moveToNext_whenNotOutOfBounds_returnsTrue() throws Exception {
        subject.moveToFirst();

        assertThat(subject.moveToNext()).isTrue();
        assertThat(subject.moveToNext()).isTrue();
    }

    @Test
    public void moveToPosition_whenOutOfBounds_returnsFalse() throws Exception {
        assertThat(subject.moveToPosition(3)).isFalse();
        assertThat(subject.moveToPosition(-1)).isFalse();
    }

    @Test
    public void moveToPosition_whenOutOfBounds_doesNotMoveEitherCursorPosition() throws Exception {
        subject.moveToFirst();

        subject.moveToPosition(3);

        assertThat(cursor.getPosition()).isEqualTo(1);
        assertThat(subject.getPosition()).isEqualTo(0);
    }

    @Test
    public void moveToPosition_whenNotOutOfBounds_returnsTrue() throws Exception {
        assertThat(subject.moveToPosition(0)).isTrue();
        assertThat(subject.moveToPosition(1)).isTrue();
        assertThat(subject.moveToPosition(2)).isTrue();
    }

    @Test
    public void moveToPosition_movesWrappedCursorToItemWithEqualPosition() throws Exception {
        subject.moveToPosition(0);
        assertThat(cursor.getPosition()).isEqualTo(1);

        subject.moveToPosition(1);
        assertThat(cursor.getPosition()).isEqualTo(0);

        subject.moveToPosition(2);
        assertThat(cursor.getPosition()).isEqualTo(2);
    }

    @Test
    public void getPosition_returnsCurrentPosition() throws Exception {
        subject.moveToPosition(1);
        assertThat(subject.getPosition()).isEqualTo(1);
    }

    @Test
    public void getMappedPosition_returnsCurrentMappedPosition() throws Exception {
        subject.moveToPosition(1);
        assertThat(subject.getMappedPosition(1)).isEqualTo(22);
    }

    private Cursor setupWrappedCursor(Object[][] rows) {
        MatrixCursor cursor = new MatrixCursor(new String[]{TestSortableCursorWrapper.COLUMN_NAME_POSITION});
        for (Object[] row : rows) {
            cursor.addRow(row);
        }
        return cursor;
    }
}