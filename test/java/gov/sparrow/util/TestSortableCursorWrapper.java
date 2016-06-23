package gov.sparrow.util;

import android.database.Cursor;
import gov.sparrow.adapter.helpers.SortableCursorWrapper;

public class TestSortableCursorWrapper extends SortableCursorWrapper {

    public static String COLUMN_NAME_POSITION = "position";

    public TestSortableCursorWrapper(Cursor cursor) {
        super(cursor);
        while (getWrappedCursor().moveToNext()) {
            int cursorInt = getWrappedCursor().getInt(getWrappedCursor().getColumnIndexOrThrow(COLUMN_NAME_POSITION));
            positions.put(cursorInt, getWrappedCursor().getPosition());
        }
    }

}
