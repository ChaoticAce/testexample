package gov.sparrow.adapter;

import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.contracts.ActionContract;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.SearchContract;
import gov.sparrow.util.TimeUtil;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import javax.inject.Inject;

import static org.fest.assertions.api.ANDROID.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SparrowTestRunner.class)
public class SearchResultsListAdapterTest {

    @Inject TimeUtil timeUtil;
    @Mock Cursor cursor;
    private SearchResultsListAdapter subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        when(timeUtil.getUpdatedTime(anyString())).thenReturn("20 April 2016 12:03");
        when(timeUtil.getFormattedDate(anyString())).thenReturn("25 MAY");
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_TITLE)).thenReturn(4343);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_BODY)).thenReturn(2342);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_CREATED_AT)).thenReturn(1212);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.ALIAS_NAME_NOTEBOOK_TITLE)).thenReturn(5555);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED)).thenReturn(8565);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_TYPE)).thenReturn(8766);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_DUE_DATE)).thenReturn(7777);

        subject = new SearchResultsListAdapter(null, cursor, 0, timeUtil);

    }

    @Test
    public void bindView_shouldInflateNoteSearchResults() {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);

        view.findViewById(R.id.search_list_item_body).setVisibility(View.GONE);
        view.findViewById(R.id.search_list_item_date).setVisibility(View.GONE);
        view.findViewById(R.id.search_list_item_notebook_name).setVisibility(View.GONE);
        view.findViewById(R.id.search_list_item_due_date).setVisibility(View.VISIBLE);

        when(cursor.getString(8766)).thenReturn(NoteContract.SearchableNotes.TABLE_NAME);
        when(cursor.getString(4343)).thenReturn("a good title");
        when(cursor.getString(2342)).thenReturn("Not a good body");
        when(cursor.getString(1212)).thenReturn("2016-04-20 16:03:04.159");
        when(cursor.getString(5555)).thenReturn("notebook");
        when(cursor.getString(7777)).thenReturn("2016-05-25 00:00:00.000");

        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat(((TextView) view.findViewById(R.id.search_list_item_title))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_title))).hasText("a good title");

        assertThat((TextView) view.findViewById(R.id.search_list_item_body)).isVisible();
        assertThat((TextView) view.findViewById(R.id.search_list_item_body)).hasText("Not a good body");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_date))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_date))).hasText("20 April 2016 12:03");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_notebook_name))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_notebook_name))).hasText("notebook");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_due_date))).isGone();
    }

    @Test
    public void bindView_shouldInflateActionSearchResults_whenActionHasNote() {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);

        view.findViewById(R.id.search_list_item_body).setVisibility(View.INVISIBLE);
        view.findViewById(R.id.search_list_item_date).setVisibility(View.VISIBLE);
        view.findViewById(R.id.search_list_item_notebook_name).setVisibility(View.VISIBLE);
        view.findViewById(R.id.search_list_item_due_date).setVisibility(View.GONE);

        when(cursor.getString(8766)).thenReturn(ActionContract.SearchableActions.TABLE_NAME);
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED))).thenReturn("false");
        when(cursor.getString(4343)).thenReturn("action title");
        when(cursor.getString(2342)).thenReturn("action note title");
        when(cursor.getString(7777)).thenReturn("2016-05-25 00:00:00.000");

        subject.bindView(view, RuntimeEnvironment.application, cursor);


        assertThat(((TextView) view.findViewById(R.id.search_list_item_title))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_title))).hasText("action title");

        assertThat((TextView) view.findViewById(R.id.search_list_item_body)).isVisible();
        assertThat((TextView) view.findViewById(R.id.search_list_item_body)).hasText("action note title");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_due_date))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_due_date))).hasText("25 MAY");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_date))).isGone();

        assertThat(((TextView) view.findViewById(R.id.search_list_item_notebook_name))).isGone();
    }

    @Test
    public void bindView_shouldInflateActionSearchResults_whenActionDoesNotHaveNote() {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);

        view.findViewById(R.id.search_list_item_body).setVisibility(View.VISIBLE);
        view.findViewById(R.id.search_list_item_date).setVisibility(View.VISIBLE);
        view.findViewById(R.id.search_list_item_notebook_name).setVisibility(View.VISIBLE);
        view.findViewById(R.id.search_list_item_due_date).setVisibility(View.GONE);

        when(cursor.getString(8766)).thenReturn(ActionContract.SearchableActions.TABLE_NAME);
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED))).thenReturn("false");
        when(cursor.getString(4343)).thenReturn("action title");
        when(cursor.getString(2342)).thenReturn(null);
        when(cursor.getString(7777)).thenReturn("2016-05-25 00:00:00.000");

        subject.bindView(view, RuntimeEnvironment.application, cursor);

        assertThat(((TextView) view.findViewById(R.id.search_list_item_title))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_title))).hasText("action title");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_body))).isGone();

        assertThat(((TextView) view.findViewById(R.id.search_list_item_due_date))).isVisible();
        assertThat(((TextView) view.findViewById(R.id.search_list_item_due_date))).hasText("25 MAY");

        assertThat(((TextView) view.findViewById(R.id.search_list_item_date))).isGone();

        assertThat(((TextView) view.findViewById(R.id.search_list_item_notebook_name))).isGone();
    }

    @Test
    public void bindView_forNoteResult_shouldSetCorrectIcon() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);

        when(cursor.getString(8766)).thenReturn(NoteContract.SearchableNotes.TABLE_NAME);

        subject.bindView(view, RuntimeEnvironment.application, cursor);
        Drawable expectedDrawable = RuntimeEnvironment.application.getResources().getDrawable(R.drawable.ic_note_list_header);
        Drawable actualDrawable = ((TextView) view.findViewById(R.id.search_list_item_title)).getCompoundDrawables()[0];
        Assertions.assertThat(shadowOf(actualDrawable).getCreatedFromResId()).isEqualTo(shadowOf(expectedDrawable).getCreatedFromResId());
    }

    @Test
    public void bindView_forActionResult_shouldSetCorrectIcon() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);

        when(cursor.getString(8766)).thenReturn(ActionContract.SearchableActions.TABLE_NAME);
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED))).thenReturn("false");

        subject.bindView(view, RuntimeEnvironment.application, cursor);
        Drawable expectedDrawable = RuntimeEnvironment.application.getResources().getDrawable(R.drawable.ic_action);
        Drawable actualDrawable = ((TextView) view.findViewById(R.id.search_list_item_title)).getCompoundDrawables()[0];
        Assertions.assertThat(shadowOf(actualDrawable).getCreatedFromResId()).isEqualTo(shadowOf(expectedDrawable).getCreatedFromResId());
    }

    @Test
    public void bindView_shouldBindAnActionItemAsComplete() {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED))).thenReturn("true");
        when(cursor.getString(4343)).thenReturn("a good title");
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_TYPE))).thenReturn(ActionContract.SearchableActions.TABLE_NAME);

        subject.bindView(view, RuntimeEnvironment.application, cursor);

        TextView actionBody = ((TextView) view.findViewById(R.id.search_list_item_title));

        assertThat(actionBody).hasText("a good title");
        assertThat(actionBody).hasPaintFlags(actionBody.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @Test
    public void bindView_shouldBindAnActionItemAsIncomplete() {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED))).thenReturn("false");
        when(cursor.getString(4343)).thenReturn("a good title");
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_TYPE))).thenReturn(ActionContract.SearchableActions.TABLE_NAME);

        subject.bindView(view, RuntimeEnvironment.application, cursor);
        TextView actionBody = ((TextView) view.findViewById(R.id.search_list_item_title));

        assertThat(actionBody).hasText("a good title");
        assertThat(actionBody).hasPaintFlags(actionBody.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @Test
    public void bindActionView_whenBodyIsNull_shouldHideNoteTitleAndDrawable() throws Exception {
        View view = LayoutInflater.from(RuntimeEnvironment.application).
                inflate(R.layout.search_list_item, null, false);

        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_COMPLETED))).thenReturn("false");
        when(cursor.getString(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_TYPE))).thenReturn(ActionContract.SearchableActions.TABLE_NAME);
        when(cursor.getString(cursor.getColumnIndexOrThrow(SearchContract.SearchableColumns.SHARED_COLUMN_NAME_BODY))).thenReturn(null);

        subject.bindView(view, RuntimeEnvironment.application, cursor);

        TextView actionBody = ((TextView) view.findViewById(R.id.search_list_item_body));
        assertThat(actionBody).isGone();
    }

}