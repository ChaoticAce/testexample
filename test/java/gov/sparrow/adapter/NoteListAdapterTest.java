package gov.sparrow.adapter;

import android.content.ClipData;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.listeners.NoteTouchListenerFactory;
import gov.sparrow.util.TimeUtil;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class NoteListAdapterTest {

    @Mock Cursor cursor;
    @Mock NoteTouchListenerFactory noteTouchListenerFactory;
    @Inject TimeUtil timeUtil;
    @Captor ArgumentCaptor<ClipData> dragDataCaptor;
    private NoteListAdapter subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        when(timeUtil.getUpdatedTime(anyString())).thenReturn("20 April 2016 12:03");

        when(cursor.getColumnIndexOrThrow(NoteContract.Note._ID)).thenReturn(6363);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_TITLE)).thenReturn(4343);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_BODY)).thenReturn(2342);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.ALIAS_NAME_NOTEBOOK_TITLE)).thenReturn(3333);
        when(cursor.getColumnIndex(NoteContract.Note.COLUMN_NAME_CREATED_AT)).thenReturn(1212);
        when(cursor.getLong(6363)).thenReturn(343L);
        when(cursor.getString(4343)).thenReturn("Not a good title");
        when(cursor.getString(2342)).thenReturn("Not a good body");
        when(cursor.getString(3333)).thenReturn("notebook title");
        when(cursor.getString(1212)).thenReturn("2016-04-20 16:03:04.159");

        subject = new NoteListAdapter(null, cursor, 0, timeUtil, noteTouchListenerFactory);
    }

    @Test
    public void bindView() {
        View view = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.note_list_item, null, false);

        subject.bindView(view, null, cursor);

        assertThat(((TextView) view.findViewById(R.id.note_list_item_title))).hasText("Not a good title");
        assertThat(((TextView) view.findViewById(R.id.note_list_item_body))).hasText("Not a good body");
        assertThat(((TextView) view.findViewById(R.id.note_list_item_notebook_name))).hasText("notebook title");
        assertThat(((TextView) view.findViewById(R.id.note_list_item_date))).hasText("20 April 2016 12:03");
    }

    @Test
    public void noteGrip_shouldHaveTouchListenerForTouch() throws Exception {
        View itemView = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.note_list_item, null, false);

        subject.bindView(itemView, RuntimeEnvironment.application, cursor);

        verify(noteTouchListenerFactory).create(itemView, 343L);
    }

    @Test
    public void longClick_shouldHaveListener() throws Exception {
        View itemView = LayoutInflater.from(RuntimeEnvironment.application).inflate(R.layout.note_list_item, null, false);

        subject.bindView(itemView, RuntimeEnvironment.application, cursor);

        verify(noteTouchListenerFactory).create(itemView, 343L);
    }
}