package gov.sparrow.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import gov.sparrow.SparrowTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ListPositionHelperTest {

    private final String tableName = "test table name";
    private final String columnNamePosition = "test column name";
    private final String columnNameId = "test column id";
    private final String[] selectionArgs = new String[]{"2"};

    @Mock SQLiteDatabase db;
    @Mock Cursor cursor;
    private ListPositionHelper subject;

    @Before
    public void setUp() {
        initMocks(this);
        subject = new ListPositionHelper();
    }

    @Test
    public void updateActionListPosition_whenEndPositionGreaterThanStart_shouldShiftActionsBetweenPositionsUpOne() throws Exception {
        ContentValues contentValues = new ContentValues();

        setupForUpdateActionListPosition(1, 3, selectionArgs, contentValues);

        subject.update(db, contentValues, tableName, columnNamePosition, columnNameId, selectionArgs);
        InOrder inOrder = inOrder(db);

        inOrder.verify(db).beginTransaction();
        inOrder.verify(db).execSQL("UPDATE " + tableName + " SET " +
                columnNamePosition + " = (" +
                columnNamePosition + " - 1)" +
                " WHERE " + columnNamePosition + " <= 3 AND " +
                columnNamePosition + " > 1;");
        inOrder.verify(db).update(anyString(), any(ContentValues.class), anyString(), any(String[].class));
        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();
    }

    @Test
    public void updateActionListPosition_whenEndPositionLessThanStart_shouldShiftActionsBetweenPositionsDownOne() throws Exception {
        ContentValues contentValues = new ContentValues();
        setupForUpdateActionListPosition(5, 3, selectionArgs, contentValues);

        subject.update(db, contentValues, tableName, columnNamePosition, columnNameId, selectionArgs);

        InOrder inOrder = inOrder(db);
        inOrder.verify(db).beginTransaction();
        inOrder.verify(db).execSQL("UPDATE " + tableName + " SET " +
                columnNamePosition + " = (" +
                columnNamePosition + " + 1)" +
                " WHERE " + columnNamePosition + " >= 3 AND " +
                columnNamePosition + " < 5;");
        inOrder.verify(db).update(anyString(), any(ContentValues.class), anyString(), any(String[].class));
        inOrder.verify(db).setTransactionSuccessful();
        inOrder.verify(db).endTransaction();
    }

    @Test
    public void updateActionListPosition_whenEndPositionEqualsStart_shiftsNoRows() throws Exception {
        int position = 3;
        ContentValues contentValues = new ContentValues();
        setupForUpdateActionListPosition(position, position, selectionArgs, contentValues);

        subject.update(db, contentValues, tableName, columnNamePosition, columnNameId, selectionArgs);

        verify(db, never()).execSQL(anyString());
    }

    @Test
    public void updateActionListPosition_returnsNumberOfRowsAffectedIncludingShiftedActions() throws Exception {
        ContentValues contentValues = new ContentValues();
        contentValues.put(columnNamePosition, 3);

        when(db.update(
                anyString(),
                any(ContentValues.class),
                anyString(),
                any(String[].class)
        )).thenReturn(1);

        when(db.query(tableName,
                new String[]{columnNamePosition},
                columnNameId + "=?",
                selectionArgs, null, null, null)).thenReturn(cursor);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndexOrThrow(columnNamePosition)).thenReturn(10);
        when(cursor.getInt(10)).thenReturn(9);

        int updateCount = subject.update(db, contentValues, tableName, columnNamePosition, columnNameId, selectionArgs);
        assertThat(updateCount).isEqualTo(7);
    }

    private void setupForUpdateActionListPosition(int startPosition, int endPosition, String[] selectionArgs, ContentValues contentValues) {
        contentValues.put(columnNamePosition, endPosition);

        when(db.query(
                tableName,
                new String[]{columnNamePosition},
                columnNameId + "=?",
                selectionArgs,
                null,
                null,
                null
        )).thenReturn(cursor);

        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndexOrThrow(columnNamePosition)).thenReturn(10);
        when(cursor.getInt(10)).thenReturn(startPosition);
    }

}