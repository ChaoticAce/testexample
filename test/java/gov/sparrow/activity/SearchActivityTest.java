package gov.sparrow.activity;

import android.content.AsyncTaskLoader;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;

import org.fest.assertions.api.ANDROID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.util.ActivityController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.adapter.SearchResultsListAdapter;
import gov.sparrow.contracts.ActionContract;
import gov.sparrow.contracts.NoteContract;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.database.SparrowRoboCursor;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.util.TimeUtil;

import static gov.sparrow.activity.SearchActivity.BUNDLE_QUERY_KEY;
import static gov.sparrow.activity.SearchActivity.LOADER_ID_SEARCH_ACTIVITY;
import static gov.sparrow.activity.SearchActivity.RESULT_ACTION_ID;
import static gov.sparrow.activity.SearchActivity.RESULT_NOTEBOOK_ID;
import static gov.sparrow.activity.SearchActivity.RESULT_NOTE_ID;
import static gov.sparrow.activity.SearchActivity.RESULT_QUERY_ARGUMENTS;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SparrowTestRunner.class)
public class SearchActivityTest {

    @Mock private Loader loader;
    @Mock private Cursor cursor;
    @Mock TimeUtil timeUtil;
    @Mock SparrowProvider contentProvider;
    private SearchActivity subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProvider);
        subject = Robolectric.setupActivity(SearchActivity.class);
    }

    @Test
    public void onCreate_shouldLoadPreviousQuery_withPreviousQueryIntent() {
        Intent testIntent = new Intent();
        testIntent.putExtra(BUNDLE_QUERY_KEY, "test query");

        subject = Robolectric.buildActivity(SearchActivity.class)
                .withIntent(testIntent)
                .create()
                .get();

        assertThat(subject.searchView.getQuery().toString()).isEqualTo("test query");
    }

    @Test
    public void onCreateLoader_shouldCreateCursorLoader_withQueryFromBundle() {
        Bundle expectedBundle = new Bundle();
        expectedBundle.putString(BUNDLE_QUERY_KEY, "some awesome text");

        CursorLoader cursorLoader = (CursorLoader) subject.onCreateLoader(1, expectedBundle);
        assertThat(cursorLoader.getSelection()).isEqualTo(null);
        assertThat(cursorLoader.getSelectionArgs()).isEqualTo(new String[]{"some awesome text"});
    }

    @Test
    public void onCreate_withOrientationChange_shouldLoadPreviousQuery() {
        Bundle saveInstanceBundle = new Bundle();
        saveInstanceBundle.putString(RESULT_QUERY_ARGUMENTS, "test query");

        ActivityController<SearchActivity> controller = Robolectric.buildActivity(SearchActivity.class);
        subject = controller.create(saveInstanceBundle).start().resume().visible().get();

        assertThat(subject.searchView.getQuery().toString()).isEqualTo("test query");
    }

    @Test
    public void onLoadFinished_whenAdapterIsNull_shouldAttachCursorToAdapter() {
        assertThat(subject.searchResultsList.getAdapter()).isNull();
        subject.onLoadFinished(loader, cursor);
        assertThat(((CursorAdapter) subject.searchResultsList.getAdapter()).getCursor()).isEqualTo(cursor);
    }

    @Test
    public void onLoadFinished_setsSearchResultsEmptyView() throws Exception {
        subject.onLoadFinished(loader, cursor);
        assertThat(subject.searchResultsList.getEmptyView()).isEqualTo(subject.searchResultsEmptyView);
    }

    @Test
    public void onLoadFinished_whenAdapterIsNotNull_shouldSwapNewData() throws Exception {
        subject.searchResultsList.setAdapter(new SearchResultsListAdapter(subject, null, 0, null));

        subject.onLoadFinished(loader, cursor);

        assertThat(((CursorAdapter) subject.searchResultsList.getAdapter()).getCursor()).isEqualTo(cursor);
    }

    @Test
    public void onSearchItemClicked_shouldFinishSearchActivity() {
        populateSearchList();

        subject.searchResultsList.performItemClick(null, 1, 0);

        assert (shadowOf(subject).isFinishing());
    }

    @Test
    public void onSearchItemClicked_whenNoteClicked_shouldCallSetResultWithNote() {
        populateSearchList();

        subject.searchResultsList.performItemClick(null, 1, 0);

        assertThat(shadowOf(subject).getResultCode()).isEqualTo(MainActivity.SEARCH_RESULT_COMPLETED_WITH_NOTE);

    }

    @Test
    public void onSearchItemClicked_whenActionClicked_shouldCallSetResultWithAction() {
        populateSearchList();

        subject.searchResultsList.performItemClick(null, 2, 0);

        assertThat(shadowOf(subject).getResultCode()).isEqualTo(MainActivity.SEARCH_RESULT_COMPLETED_WITH_ACTION);
    }

    @Test
    public void onSearchItemClicked_whenBackPressed_shouldCloseSearch() {
        populateSearchList();

        subject.onBackPressed();

        assertThat(shadowOf(subject).getResultCode()).isEqualTo(MainActivity.SEARCH_CANCELLED);
    }

    @Test
    public void onSearchItemClicked_whenActionClicked_shouldCreateIntentForAction() throws Exception {
        populateSearchList();

        subject.searchResultsList.performItemClick(null, 2, 0);

        assertThat(shadowOf(subject).getResultIntent().getLongExtra(RESULT_ACTION_ID, 0L)).isEqualTo(1L);
    }

    @Test
    public void onSearchItemClicked_whenNoteClicked_shouldCreateIntentForNote() throws Exception {
        populateSearchList();

        subject.searchResultsList.performItemClick(null, 1, 0);

        assertThat(shadowOf(subject).getResultIntent().getLongExtra(RESULT_NOTE_ID, 0L)).isEqualTo(2L);
        assertThat(shadowOf(subject).getResultIntent().getLongExtra(RESULT_NOTEBOOK_ID, 0L)).isEqualTo(22L);
    }

    @Test
    public void onBackPressed_shouldReturnCurrentQuery() throws Exception {
        subject.searchText.setText("search text");

        subject.onBackPressed();

        assertThat(shadowOf(subject).getResultIntent().getStringExtra(RESULT_QUERY_ARGUMENTS)).isEqualTo("search text");
    }

    @Test
    public void onSearchCloseButtonClick_shouldClearSearchText() throws Exception {
        subject.searchResultsList.setAdapter(new SearchResultsListAdapter(null, cursor, 0, timeUtil));
        subject.searchText.setText("This is my search!");
        subject.searchCloseButton.performClick();
        assertThat(subject.searchText.getText().toString()).isEmpty();
    }

    @Test
    public void onSearchCloseButtonClick_whenThereAreResults_shouldClearSearchResultList() throws Exception {
        SparrowRoboCursor cursor = new SparrowRoboCursor();
        cursor.setColumnNames(Arrays.asList(
                NoteContract.Note._ID,
                NoteContract.Note.COLUMN_NAME_TITLE,
                NoteContract.Note.COLUMN_NAME_BODY,
                NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID
                )
        );
        Object[][] results = new Object[][]{
                new Object[]{1L, "title 1", "boom 1", "1"},
                new Object[]{2L, "title 2", "boom 2", "2"}
        };
        cursor.setResults(results);
        subject.searchResultsList.setAdapter(new SearchResultsListAdapter(null, cursor, 0, timeUtil));
        ANDROID.assertThat(subject.searchResultsList).hasCount(2);

        subject.searchCloseButton.performClick();

        ANDROID.assertThat(subject.searchResultsList).hasCount(0);
    }

    @Test
    public void onSearchCloseButtonClick_whenThereAreNoResults_DoesNothing() throws Exception {
        subject.searchCloseButton.performClick();

        //Does not go boom
    }

    @Test
    public void whenQuerySizeDecreasesToOneCharacter_whenThereAreResults_shouldClearSearchResultsList() throws Exception {
        SparrowRoboCursor cursor = new SparrowRoboCursor();
        cursor.setColumnNames(Arrays.asList(
                NoteContract.Note._ID,
                NoteContract.Note.COLUMN_NAME_TITLE,
                NoteContract.Note.COLUMN_NAME_BODY,
                NoteContract.Note.COLUMN_NAME_NOTEBOOK_ID
                )
        );
        Object[][] results = new Object[][]{
                new Object[]{1L, "title 1", "boom 1", "1"},
                new Object[]{2L, "title 2", "boom 2", "2"}
        };
        cursor.setResults(results);
        subject.searchResultsList.setAdapter(new SearchResultsListAdapter(null, cursor, 0, timeUtil));
        ANDROID.assertThat(subject.searchResultsList).hasCount(2);

        subject.searchText.setText("a");

        ANDROID.assertThat(subject.searchResultsList).hasCount(0);
    }

    @Test
    public void whenQuerySizeDecreasesToOneCharacter_andThereAreNoResults_shouldNotCrash() throws Exception {
        subject.searchText.setText("a");

        //Does not go boom
    }

    @Test
    public void whenQuerySizeLessThan2Characters_shouldNotSaveQuery() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_QUERY_KEY, "search text");
        subject.onCreateLoader(0, bundle);
        subject.searchText.setText("a");

        subject.onBackPressed();

        assertThat(shadowOf(subject).getResultIntent().getStringExtra(RESULT_QUERY_ARGUMENTS)).isEqualTo(null);
    }

    @Test
    public void whenQuerySubmitted_shouldRunQuery() throws Exception {
        boolean result = subject.onQueryTextSubmit("search term");
        assertThatQueryLoaded("search term");
        assertThat(result).isTrue();
    }

    @Test
    public void whenQueryTextChange_andQueryTextGreaterThan1_shouldLoadQuery() throws Exception {
        boolean result = subject.onQueryTextChange("search term");
        assertThatQueryLoaded("search term");
        assertThat(result).isTrue();
    }

    @Test
    public void whenQueryTextChange_andQueryTextLessThan2_shouldNotLoadQuery() throws Exception {
        boolean result = subject.onQueryTextChange("s");
        AsyncTaskLoader<Cursor> loader = (AsyncTaskLoader) subject.getLoaderManager().getLoader(LOADER_ID_SEARCH_ACTIVITY);
        assertThat(result).isFalse();
        assertThat(loader).isNull();
    }

    @Test
    public void whenQueryTextChange_andQueryTextLessThan2_shouldRemoveSearchResultsEmptyView() throws Exception {
        subject.searchResultsList.setEmptyView(subject.searchResultsEmptyView);

        subject.onQueryTextChange("s");

        assertThat(subject.searchResultsList.getEmptyView()).isNull();
    }

    @Test
    public void onSaveInstanceState_shouldSaveQueryArgumentsToBundle(){
        subject.searchView.setQuery("test search", false);
        Bundle bundle = new Bundle();

        subject.onSaveInstanceState(bundle);

        assertThat(bundle.get(RESULT_QUERY_ARGUMENTS)).isEqualTo("test search");
    }

    private void populateSearchList() {
        Note note0 = NoteBuilder.noteBuilder()
                .id(1L)
                .title("title 1")
                .body("boom 1")
                .notebookId(11L)
                .build();
        Note note1 = NoteBuilder.noteBuilder()
                .id(2L)
                .title("title 2")
                .body("boom 2")
                .notebookId(22L)
                .build();
        Cursor actionCursor = getCursorForAction();
        List<Cursor> cursors = getCursorList(Arrays.asList(note0, note1));
        cursors.add(actionCursor);
        ArrayAdapter<Cursor> adapter = new ArrayAdapter<>(subject, R.layout.note_list_item, R.id.note_list_item_title, cursors);
        subject.searchResultsList.setAdapter(adapter);
        shadowOf(subject.searchResultsList).populateItems();
    }

    private List<Cursor> getCursorList(List<Note> notes) {
        List<Cursor> cursors = new ArrayList<>(notes.size());
        for (Note note : notes) {
            cursors.add(getCursorForNote(note));
        }
        return cursors;
    }

    private Cursor getCursorForNote(Note note) {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_ID)).thenReturn(0);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_TITLE)).thenReturn(1);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_BODY)).thenReturn(2);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_CREATED_AT)).thenReturn(3);
        when(cursor.getColumnIndexOrThrow(NoteContract.SearchableNotes.SHARED_COLUMN_NAME_NOTEBOOK_ID)).thenReturn(4);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.ALIAS_NAME_NOTEBOOK_TITLE)).thenReturn(5);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.COLUMN_NAME_TYPE)).thenReturn(6);
        when(cursor.getLong(0)).thenReturn(note.getId());
        when(cursor.getString(1)).thenReturn(note.getTitle());
        when(cursor.getString(2)).thenReturn(note.getBody());
        when(cursor.getString(3)).thenReturn("2016-04-20 16:03:04.159");
        when(cursor.getLong(4)).thenReturn(note.getNotebookId());
        when(cursor.getString(5)).thenReturn("notebook");
        when(cursor.getString(6)).thenReturn(NoteContract.SearchableNotes.TABLE_NAME);

        return cursor;
    }

    private Cursor getCursorForAction() {
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_ID)).thenReturn(0);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_TITLE)).thenReturn(1);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_BODY)).thenReturn(2);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_CREATED_AT)).thenReturn(3);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_NOTEBOOK_ID)).thenReturn(4);
        when(cursor.getColumnIndexOrThrow(NoteContract.Note.ALIAS_NAME_NOTEBOOK_TITLE)).thenReturn(5);
        when(cursor.getColumnIndexOrThrow(ActionContract.SearchableActions.SHARED_COLUMN_NAME_TYPE)).thenReturn(6);
        when(cursor.getLong(0)).thenReturn(1L);
        when(cursor.getString(1)).thenReturn("action title");
        when(cursor.getString(3)).thenReturn("2016-04-20 16:03:04.159");
        when(cursor.getString(6)).thenReturn(ActionContract.SearchableActions.TABLE_NAME);

        return cursor;
    }

    private void assertThatQueryLoaded(String searchTerm) {
        AsyncTaskLoader<Cursor> loader = (AsyncTaskLoader) subject.getLoaderManager().getLoader(LOADER_ID_SEARCH_ACTIVITY);
        CursorLoader loader1 = (CursorLoader) loader;
        assertThat(loader1.getSelectionArgs()[0]).isEqualTo(searchTerm);
        assertThat(loader1.isStarted()).isTrue();
    }

}
