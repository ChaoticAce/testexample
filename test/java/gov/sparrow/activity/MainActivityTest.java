package gov.sparrow.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.view.Gravity;
import android.view.View;

import org.fest.assertions.api.ANDROID;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.util.ActivityController;

import java.util.List;

import javax.inject.Inject;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.TestSparrowApplication;
import gov.sparrow.contracts.SparrowContract;
import gov.sparrow.datasync.SparrowRestoreManager;
import gov.sparrow.fragment.ActionPaneFragment;
import gov.sparrow.fragment.NoteListFragment;
import gov.sparrow.fragment.NotePaneFragment;
import gov.sparrow.fragment.NotebookListFragment;
import gov.sparrow.fragment.RestoreDialogFragment;
import gov.sparrow.managers.SaveManager;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.provider.SparrowProvider;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.util.TimeUtil;

import static gov.sparrow.activity.MainActivity.EMPTY_VIEW_VISIBILITY;
import static gov.sparrow.activity.MainActivity.FRAGMENT_ACTION_PANE_TAG;
import static gov.sparrow.activity.MainActivity.FRAGMENT_NOTE_PANE_TAG;
import static gov.sparrow.activity.MainActivity.NOTE_LIST_VISIBILITY;
import static gov.sparrow.activity.MainActivity.PROGRESS_DIALOG_TAG;
import static gov.sparrow.activity.MainActivity.SEARCH_REQUEST_CODE;
import static gov.sparrow.activity.MainActivity.SEARCH_RESULT_COMPLETED_WITH_ACTION;
import static gov.sparrow.activity.MainActivity.SEARCH_RESULT_COMPLETED_WITH_NOTE;
import static gov.sparrow.activity.SearchActivity.BUNDLE_QUERY_KEY;
import static gov.sparrow.activity.SearchActivity.RESULT_NOTEBOOK_ID;
import static gov.sparrow.activity.SearchActivity.RESULT_NOTE_ID;
import static gov.sparrow.activity.SearchActivity.RESULT_QUERY_ARGUMENTS;
import static gov.sparrow.contracts.NotebookContract.Notebook;
import static gov.sparrow.database.SparrowDatabaseHelper.BLANK_NOTE_TITLE;
import static gov.sparrow.fragment.NoteListFragment.HIGHLIGHTED_NOTE_ID;
import static gov.sparrow.fragment.NoteListFragment.NOTEBOOK_ID;
import static org.fest.assertions.api.ANDROID.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SparrowTestRunner.class)
public class MainActivityTest {

    private final Note testNote = NoteBuilder.noteBuilder()
            .id(1L)
            .title("test title")
            .body("test body")
            .notebookId(2L)
            .lastSaved("test timestamp")
            .build();

    @Inject TimeUtil timeUtil;
    @Inject NoteRepository noteRepository;
    @Inject SaveManager saveManager;
    @Mock SparrowProvider contentProvider;
    @Mock Cursor notebookQueryCursor;
    @Captor ArgumentCaptor<NoteRepository.CreateNoteListener> createCaptor;
    @Captor ArgumentCaptor<NoteRepository.QueryNoteListener> queryCaptor;
    @Captor ArgumentCaptor<NoteRepository.UpdateNoteListener> updateCaptor;
    @Captor ArgumentCaptor<SaveManager.SaveCompleteListener> saveCaptor;
    private MainActivity subject;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ((TestSparrowApplication) RuntimeEnvironment.application).getAppComponent().inject(this);

        when(timeUtil.getUpdatedTime(anyString())).thenReturn("updated timestamp");
        when(timeUtil.getTimeNow()).thenReturn("current timestamp");

        when(contentProvider.query(Notebook.CONTENT_URI, null, null, null, null)).thenReturn(notebookQueryCursor);
        ShadowContentResolver.registerProvider(SparrowContract.SPARROW_CONTENT_AUTHORITY, contentProvider);
    }

    @Test
    public void onCreate_withNotesInTheDatabase_shouldLoadLastSavedNote() {
        setupGetLastSavedNote_forOnCreate();

        NotePaneFragment notePane = (NotePaneFragment) getWorkspacePaneFragment();
        assertThat(notePane.noteTitle.getText().toString()).isEqualTo("test title");
        assertThat(notePane.noteBody.getText().toString()).isEqualTo("test body");
        assertThat(notePane.updateTimestamp.getText().toString()).isEqualTo("Last Saved updated timestamp");
    }

    @Test
    public void onCreate_shouldLoadNotebooksList() {
        setupGetLastSavedNote_forOnCreate();
        assertThat(subject.getSupportFragmentManager().findFragmentById(R.id.notebook_list_fragment)).isInstanceOf(NotebookListFragment.class);
    }

    @Test
    public void onCreate_shouldLoadNotesList() throws Exception {
        setupGetLastSavedNote_forOnCreate();
        assertThat(getNoteListFragment()).isInstanceOf(NoteListFragment.class);
    }

    @Test
    public void onCreate_shouldLoadNotesPane() {
        setupGetLastSavedNote_forOnCreate();
        assertThat(getWorkspacePaneFragment()).isInstanceOf(NotePaneFragment.class);
        assertThat(subject.workspaceEmptyView).isGone();
    }

    @Test
    public void onCreate_whenThereIsNoNote_shouldNotLoadNotesPane() {
        subject = Robolectric.setupActivity(MainActivity.class);

        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(null);
        reset(noteRepository);

        assertThat(getWorkspacePaneFragment()).isNull();
        assertThat(subject.workspaceEmptyView).isVisible();
    }

    @Test
    public void onCreate_whenInPortraitMode_shouldInitializeDrawer() {
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        setupGetLastSavedNote_forOnCreate();

        assertThat(subject.drawerLayout).isNotNull();
    }

    @Test
    public void onCreate_whenInLandscapeMode_shouldNotInitializeDrawer() {
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        setupGetLastSavedNote_forOnCreate();

        assertThat(subject.drawerLayout).isNull();
    }

    @Test
    public void onCreate_whenSaveInstanceState_shouldRestoreNoteListVisibility() throws Exception {
        Bundle saveInstanceState = new Bundle();
        saveInstanceState.putBoolean(NOTE_LIST_VISIBILITY, true);

        subject = Robolectric.buildActivity(MainActivity.class).create(saveInstanceState).start().resume().visible().get();

        assertThat(subject.noteListFragment).isVisible();
    }

    @Test
    public void onCreate_whenSaveInstanceState_shouldRestoreEmptyViewVisibility() throws Exception {
        Bundle saveInstanceState = new Bundle();
        saveInstanceState.putBoolean(EMPTY_VIEW_VISIBILITY, true);

        subject = Robolectric.buildActivity(MainActivity.class).create(saveInstanceState).start().resume().visible().get();

        assertThat(subject.workspaceEmptyView).isVisible();
    }

    @Test
    public void onSaveInstanceState_shouldSaveNoteListVisibility() throws Exception {
        Bundle saveInstanceState = new Bundle();
        subject = Robolectric.setupActivity(MainActivity.class);
        subject.noteListFragment.setVisibility(View.GONE);

        subject.onSaveInstanceState(saveInstanceState);

        assertThat(saveInstanceState.getBoolean(NOTE_LIST_VISIBILITY)).isFalse();
    }

    @Test
    public void onSaveInstanceState_shouldSaveEmptyViewVisibility() throws Exception {
        Bundle saveInstanceState = new Bundle();
        subject = Robolectric.setupActivity(MainActivity.class);
        subject.workspaceEmptyView.setVisibility(View.VISIBLE);

        subject.onSaveInstanceState(saveInstanceState);

        assertThat(saveInstanceState.getBoolean(MainActivity.EMPTY_VIEW_VISIBILITY)).isTrue();
    }

    @Test
    public void onCreateNewNoteClicked_shouldInsertBlankNoteIntoSelectedNotebook() {
        setupGetLastSavedNote_forOnCreate();
        subject.onCreateNewNoteClicked(3483483L);

        ArgumentCaptor<NoteRepository.CreateNoteListener> createCaptor = ArgumentCaptor.forClass(NoteRepository.CreateNoteListener.class);
        verify(noteRepository).asyncCreateNote(eq(3483483L), eq(BLANK_NOTE_TITLE), eq(""), createCaptor.capture());
        createCaptor.getValue().onCreateNoteComplete(123L);

        verify(noteRepository).asyncGetNote(eq(123L), any(NoteRepository.QueryNoteListener.class));
    }

    @Test
    public void onCreateNewNoteClicked_shouldStartNewNotePane() throws Exception {
        setupGetLastSavedNote_forOnCreate();
        subject.onCreateNewNoteClicked(215L);

        ArgumentCaptor<NoteRepository.CreateNoteListener> createCaptor = ArgumentCaptor.forClass(NoteRepository.CreateNoteListener.class);
        verify(noteRepository).asyncCreateNote(eq(215L), eq(BLANK_NOTE_TITLE), eq(""), createCaptor.capture());
        createCaptor.getValue().onCreateNoteComplete(123L);

        ArgumentCaptor<NoteRepository.QueryNoteListener> queryCaptor = ArgumentCaptor.forClass(NoteRepository.QueryNoteListener.class);
        verify(noteRepository).asyncGetNote(eq(123L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(1L)
                        .title("test title")
                        .body("test body")
                        .notebookId(2L)
                        .lastSaved("Last Saved updated timestamp")
                        .build());

        NotePaneFragment fragment = (NotePaneFragment) getWorkspacePaneFragment();
        assertThat(fragment.getNoteId()).isEqualTo(1L);
        assertThat(fragment.noteTitle.getText().toString()).isEqualTo("test title");
        assertThat(fragment.noteBody.getText().toString()).isEqualTo("test body");
        assertThat(fragment.updateTimestamp.getText().toString()).isEqualTo("Last Saved updated timestamp");
    }

    @Test
    public void onCreateNewNoteClicked_whenInPortraitMode_shouldCloseNotePane() {
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        setupGetLastSavedNote_forOnCreate();
        subject.drawerLayout = mock(DrawerLayout.class);

        subject.onCreateNewNoteClicked(3483483L);

        ArgumentCaptor<NoteRepository.CreateNoteListener> createCaptor = ArgumentCaptor.forClass(NoteRepository.CreateNoteListener.class);
        verify(noteRepository).asyncCreateNote(eq(3483483L), eq(BLANK_NOTE_TITLE), eq(""), createCaptor.capture());
        createCaptor.getValue().onCreateNoteComplete(123L);

        ArgumentCaptor<NoteRepository.QueryNoteListener> queryCaptor = ArgumentCaptor.forClass(NoteRepository.QueryNoteListener.class);
        verify(noteRepository).asyncGetNote(eq(123L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(null);

        verify(subject.drawerLayout).closeDrawer(Gravity.LEFT);
    }

    @Test
    public void onNotebookListActionButtonClick_shouldSwitchToActionPaneFragment() {
        setupGetLastSavedNote_forOnCreate();

        subject.onNotebookListActionButtonClicked();

        assertThat(getWorkspacePaneFragment()).isInstanceOf(ActionPaneFragment.class);
        assertThat(subject.noteListFragment.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onNotebookListActionButtonClick_shouldHideEmptyView() {
        subject = Robolectric.setupActivity(MainActivity.class);

        subject.onNotebookListActionButtonClicked();

        assertThat(subject.workspaceEmptyView).isGone();
    }

    @Test
    public void onNotebookListActionButtonClick_whenInPortrait_shouldCloseDrawer() throws Exception {
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = Robolectric.setupActivity(MainActivity.class);
        subject.drawerLayout = mock(DrawerLayout.class);

        subject.onNotebookListActionButtonClicked();

        verify(subject.drawerLayout).closeDrawer(Gravity.LEFT);
    }

    @Test
    public void onNoteListItemClicked_shouldLoadNotesPane_withCorrectNote() {
        setupGetLastSavedNote_forOnCreate();

        subject.onNoteListItemClicked(2L);

        ArgumentCaptor<NoteRepository.QueryNoteListener> queryCaptor = ArgumentCaptor.forClass(NoteRepository.QueryNoteListener.class);
        verify(noteRepository).asyncGetNote(eq(2L), queryCaptor.capture());

        queryCaptor.getValue().onQueryNoteComplete(testNote);

        NotePaneFragment fragment = (NotePaneFragment) getWorkspacePaneFragment();
        assertThat(fragment.noteTitle.getText().toString()).isEqualTo("test title");
        assertThat(fragment.noteBody.getText().toString()).isEqualTo("test body");
    }

    @Test
    public void onNoteListItemClicked_whenInPortrait_shouldCloseDrawer() throws Exception {
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject = Robolectric.setupActivity(MainActivity.class);
        subject.drawerLayout = mock(DrawerLayout.class);

        subject.onNoteListItemClicked(123L);

        ArgumentCaptor<NoteRepository.QueryNoteListener> queryNoteListenerArgumentCaptor = ArgumentCaptor.forClass(NoteRepository.QueryNoteListener.class);
        verify(noteRepository).asyncGetNote(eq(123L), queryNoteListenerArgumentCaptor.capture());
        queryNoteListenerArgumentCaptor.getValue().onQueryNoteComplete(testNote);

        verify(subject.drawerLayout).closeDrawer(Gravity.LEFT);
    }

    @Test
    public void onNoteListItemClicked_whenInPortrait_andNoteAlreadyOpen_shouldCloseDrawer() throws Exception {
        RuntimeEnvironment.application.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        setupGetLastSavedNote_forOnCreate();
        subject.drawerLayout = mock(DrawerLayout.class);

        subject.onNoteListItemClicked(1L);

        verify(subject.drawerLayout).closeDrawer(Gravity.LEFT);
    }

    @Test
    public void onNoteListItemClicked_whenNoNotePaneExists_shouldNotThrowException() {
        try {
            subject = Robolectric.setupActivity(MainActivity.class);
            subject.onNoteListItemClicked(1L);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void switchingFromActionPaneToNote_shouldCreateListWithClickedOnNotebookId() throws Exception {
        setupGetLastSavedNote_forOnCreate();
        subject.onNotebookListActionButtonClicked();

        subject.onNotebookListItemClicked(Notebook.UNASSIGNED_NOTEBOOK_ID);

        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(455L)
                        .title("")
                        .body("")
                        .notebookId(9L)
                        .build());

        Long actualNotebookId = getNoteListFragment().getArguments().getLong(NOTEBOOK_ID);
        assertThat(actualNotebookId).isEqualTo((Notebook.UNASSIGNED_NOTEBOOK_ID));
    }

    @Test
    public void onNotebookListItemClicked_shouldReloadNoteWorkspace() {
        setupGetLastSavedNote_forOnCreate();

        subject.onNotebookListItemClicked(2L);

        assertNotebookNotesLoadedIntoNoteWorkspace();
    }

    @Test
    public void onNotebookListItemClicked_whenNotebookIsEmpty_shouldRemoveNotePane() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onNotebookListItemClicked(2L);
        verify(saveManager).save(anyLong(), any(Editable.class), any(Editable.class), saveCaptor.capture());
        saveCaptor.getValue().onSaveComplete();

        verify(noteRepository).asyncGetLastSavedNote(eq(2L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(null);

        assertThat(getNoteListFragment().getArguments().getLong(NOTEBOOK_ID)).isEqualTo((2L));

        assertThat(getWorkspacePaneFragment()).isNull();
        assertThat(subject.workspaceEmptyView).isVisible();
    }

    @Test
    public void onNotebookListItemClicked_whenNoNotePaneExists_shouldNotThrowException() {
        try {
            subject = Robolectric.setupActivity(MainActivity.class);
            subject.onNotebookListItemClicked(2L);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onCreate_whenNoNotePaneExists_shouldNotThrowException() throws Exception {
        try {
            subject = Robolectric.setupActivity(MainActivity.class);
        } catch (NullPointerException e) {
            Assert.fail();
        }
    }

    @Test
    public void onClickingAlreadyLoadedNoteListItem_shouldNotReloadNoteInNotePane() {
        setupGetLastSavedNote_forOnCreate();

        reset(noteRepository);

        subject.onNoteListItemClicked(1L);

        verifyZeroInteractions(noteRepository);
    }

    @Test
    public void changingOrientation_whenActionPaneIsShowing_ShouldContinueToShowActionPane() {
        ActivityController<MainActivity> controller1 = Robolectric.buildActivity(MainActivity.class).create();

        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(testNote);

        MainActivity beforeRecreation = controller1.start().resume().visible().get();
        assertThat(beforeRecreation.getSupportFragmentManager().findFragmentByTag(FRAGMENT_NOTE_PANE_TAG)).isVisible();

        beforeRecreation.findViewById(R.id.show_actions_button).performClick();
        assertThat(beforeRecreation.getSupportFragmentManager().findFragmentByTag(FRAGMENT_ACTION_PANE_TAG)).isVisible();

        /*
        * Simulating the lifecycle flow of an orientation change
        * Before an activity is destroyed and recreated,
        * a bundle is saved and passed to the new activity
        * */
        Bundle bundle = new Bundle();
        controller1.saveInstanceState(bundle);
        ActivityController<MainActivity> controller2 = Robolectric.buildActivity(MainActivity.class);
        MainActivity afterRecreation = controller2.create(bundle).start().resume().visible().get();

        assertThat(afterRecreation.getSupportFragmentManager().findFragmentByTag(FRAGMENT_ACTION_PANE_TAG)).isVisible();
    }

    @Test
    public void onNewNoteCreated_shouldCreateListWithNewNoteId() throws Exception {
        subject = Robolectric.setupActivity(MainActivity.class);

        subject.onCreateNewNoteClicked(215L);

        verify(noteRepository).asyncCreateNote(eq(215L), eq(BLANK_NOTE_TITLE), eq(""), createCaptor.capture());
        createCaptor.getValue().onCreateNoteComplete(123L);

        verify(noteRepository).asyncGetNote(eq(123L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(123L)
                        .title("")
                        .body("")
                        .build());

        assertThat(getNoteListFragment().getArguments().getLong(HIGHLIGHTED_NOTE_ID)).isEqualTo((123L));
    }

    @Test
    public void switchingNotes_shouldCreateListWithNewNoteId() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onNotebookListItemClicked(9L);

        verify(saveManager).save(anyLong(), any(Editable.class), any(Editable.class), saveCaptor.capture());
        saveCaptor.getValue().onSaveComplete();

        verify(noteRepository).asyncGetLastSavedNote(eq(9L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(215L)
                        .title("")
                        .body("")
                        .build());

        assertThat(getNoteListFragment().getArguments().getLong(HIGHLIGHTED_NOTE_ID)).isEqualTo((215L));
    }

    @Test
    public void settingUpNotePane_shouldCreateListWithLastSavedNoteId() throws Exception {
        setupGetLastSavedNote_forOnCreate();
        subject.getSupportFragmentManager().beginTransaction().remove(getWorkspacePaneFragment()).commit();

        subject.onNotebookListItemClicked(9L);

        verify(noteRepository).asyncGetLastSavedNote(eq(9L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(455L)
                        .title("")
                        .body("")
                        .build());

        assertThat(getNoteListFragment().getArguments().getLong(HIGHLIGHTED_NOTE_ID)).isEqualTo((455L));
    }

    @Test
    public void onCreate_shouldCreateAllListWithLastSavedNoteId() throws Exception {
        Note note = NoteBuilder.noteBuilder()
                .id(657L)
                .title("title 2")
                .body("boom 2")
                .build();
        subject = Robolectric.setupActivity(MainActivity.class);

        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(note);

        assertThat(getNoteListFragment().getArguments().getLong(HIGHLIGHTED_NOTE_ID)).isEqualTo((657L));
    }

    @Test
    public void onActionItemNoteLinkClicked_shouldReplaceWithNoteWorkspace() {
        setupGetLastSavedNote_forOnCreate();

        subject.onNotebookListActionButtonClicked();

        subject.onActionItemNoteLinkClicked(testNote.getId());
        verify(noteRepository).asyncGetNote(eq(testNote.getId()), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(testNote);

        Long notebookId = getNoteListFragment().getArguments().getLong(NOTEBOOK_ID);
        Long noteId = getNoteListFragment().getArguments().getLong(HIGHLIGHTED_NOTE_ID);

        assertThat(getNoteListFragment()).isVisible();
        assertThat(notebookId).isEqualTo(testNote.getNotebookId());
        assertThat(noteId).isEqualTo(testNote.getId());

        assertThat(getWorkspacePaneFragment()).isInstanceOf(NotePaneFragment.class);
        assertThat(((NotePaneFragment) getWorkspacePaneFragment()).getNoteId()).isEqualTo(testNote.getId());
    }

    @Test
    public void onActionItemNoteLinkClicked_shouldDeactivateActionViewButton() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        NotebookListFragment notebookListFragment = (NotebookListFragment) subject.getSupportFragmentManager().findFragmentById(R.id.notebook_list_fragment);
        notebookListFragment.viewActionsButton.setActivated(true);

        subject.onActionItemNoteLinkClicked(testNote.getId());
        verify(noteRepository).asyncGetNote(eq(testNote.getId()), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(testNote);

        assertThat(notebookListFragment.viewActionsButton.isActivated()).isFalse();
    }

    @Test
    public void onArchiveClicked_shouldReloadNoteWorkspace() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onArchiveCompleted();

        verify(noteRepository).asyncGetLastSavedNote(eq(0L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(215L)
                        .title("")
                        .body("")
                        .notebookId(9L)
                        .build());

        assertThat(getNoteListFragment().getNotebookId()).isEqualTo((0L));
    }

    @Test
    public void onNotebookDragEventCompleted_shouldReloadNoteWorkspace_whenNotViewingTheAllNotebook() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.getSupportFragmentManager().beginTransaction()
                .replace(R.id.note_list_fragment, NoteListFragment.newInstance(1L, 1L))
                .commit();
        subject.getSupportFragmentManager()
                .executePendingTransactions();

        subject.noteDragEventCompleted();

        verify(saveManager).save(anyLong(), any(Editable.class), any(Editable.class), saveCaptor.capture());
        saveCaptor.getValue().onSaveComplete();

        verify(noteRepository).asyncGetLastSavedNote(eq(1L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(215L)
                        .title("")
                        .body("")
                        .notebookId(9L)
                        .build());

        assertThat(getNoteListFragment().getNotebookId()).isEqualTo((1L));
    }

    @Test
    public void onSearchMenuItemClicked_withNoPreviousQuery_shouldStartSearchActivity() {
        setupGetLastSavedNote_forOnCreate();

        ShadowActivity shadowActivity = shadowOf(subject);
        shadowActivity.clickMenuItem(R.id.search_option_item);

        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);

        assertThat(shadowIntent.getComponent().getClassName()).isEqualTo(SearchActivity.class.getName());
        assertThat(shadowIntent.getStringExtra(RESULT_QUERY_ARGUMENTS)).isNull();
    }

    @Test
    public void onActivityResult_givenANote_shouldSetTheNoteWorkspace() {
        setupGetLastSavedNote_forOnCreate();

        subject.onActivityResult(SEARCH_REQUEST_CODE, SEARCH_RESULT_COMPLETED_WITH_NOTE, makeSearchResultIntent());

        Note expectedNote = NoteBuilder.noteBuilder()
                .id(33L)
                .title("test title")
                .body("test body")
                .notebookId(103L)
                .build();
        verify(noteRepository).asyncGetNote(eq(33L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(expectedNote);

        NotePaneFragment notePaneFragment = (NotePaneFragment) getWorkspacePaneFragment();
        assertThat(notePaneFragment.getNoteId()).isEqualTo(33L);

        NoteListFragment noteListFragment = getNoteListFragment();
        assertThat(noteListFragment.getNotebookId()).isEqualTo(103L);
    }

    @Test
    public void onActivityResult_givenAction_shouldSetTheActionWorkspace() throws Exception {
        subject = Robolectric.setupActivity(MainActivity.class);

        Intent intent = new Intent();
        intent.putExtra(SearchActivity.RESULT_ACTION_ID, 1L);
        intent.putExtra(RESULT_QUERY_ARGUMENTS, "previous search test query");

        subject.onActivityResult(SEARCH_REQUEST_CODE, SEARCH_RESULT_COMPLETED_WITH_ACTION, intent);

        Fragment workspacePaneFragment = getWorkspacePaneFragment();
        assertThat(workspacePaneFragment).isInstanceOf(ActionPaneFragment.class);
        assertThat(workspacePaneFragment.getArguments().getLong(ActionPaneFragment.ACTION_ID)).isEqualTo(1L);
        assertThat(subject.noteListFragment).isGone();
        assertThat(subject.workspaceEmptyView).isGone();
    }

    @Test
    public void onSearchMenuItemClicked_withPreviousQuery_shouldStartSearchActivity_withPreviousQueryIntent() {
        setupGetLastSavedNote_forOnCreate();

        subject.onActivityResult(SEARCH_REQUEST_CODE, SEARCH_RESULT_COMPLETED_WITH_NOTE, makeSearchResultIntent());

        ShadowActivity shadowActivity = shadowOf(subject);
        shadowActivity.clickMenuItem(R.id.search_option_item);

        Intent startedIntent = shadowActivity.getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);

        assertThat(shadowIntent.getStringExtra(BUNDLE_QUERY_KEY)).isEqualTo("previous search test query");
    }

    @Test
    public void onRestoreMenuItemClicked_shouldCreateRestoreDialog() {
        setupGetLastSavedNote_forOnCreate();

        ShadowActivity shadowActivity = shadowOf(subject);
        shadowActivity.clickMenuItem(R.id.restore_option_item);

        assertThat(ShadowAlertDialog.getLatestDialog()).isNotNull();
        List<Fragment> fragments = subject.getSupportFragmentManager().getFragments();
        assertThat(fragments.get(fragments.size() - 1)).isInstanceOf(RestoreDialogFragment.class);

    }

    @Test
    public void onRestoreStarted_shouldDisableSaveManager() throws Exception {
        setupGetLastSavedNote_forOnCreate();
        subject.onRestoreStarted(mock(SparrowRestoreManager.RestoreData.class));
        verify(saveManager).setEnabled(false);
    }

    @Test
    public void onRestoreStarted_shouldShowProgressDialog() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onRestoreStarted(mock(SparrowRestoreManager.RestoreData.class));

        ANDROID.assertThat(subject.getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_TAG)).isNotNull();
        ANDROID.assertThat(ShadowAlertDialog.getLatestDialog()).isNotNull();
        ANDROID.assertThat(ShadowAlertDialog.getLatestDialog()).isInstanceOf(AlertDialog.class);
    }

    @Test
    public void onRestoreComplete_shouldReloadNoteWorkspace() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onRestoreComplete();

        validateWorkspaceReplacement();
    }

    @Test
    public void onRestoreComplete_shouldDeactivateViewActionButton() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        NotebookListFragment fragment = (NotebookListFragment) subject.getSupportFragmentManager().findFragmentById(R.id.notebook_list_fragment);
        fragment.viewActionsButton.performClick();

        subject.onRestoreComplete();
        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(testNote);

        assertThat(fragment.viewActionsButton.isActivated()).isFalse();
    }

    @Test
    public void onRestoreComplete_shouldEnableSaveManager() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onRestoreComplete();
        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(testNote);

        verify(saveManager).setEnabled(true);
    }

    @Test
    public void onNotebookDelete_shouldReloadNoteWorkspace() throws Exception {
        setupGetLastSavedNote_forOnCreate();

        subject.onNotebookDelete();

        validateWorkspaceReplacement();
    }

    private void validateWorkspaceReplacement() {
        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(215L)
                        .title("")
                        .body("")
                        .notebookId(Notebook.UNASSIGNED_NOTEBOOK_ID)
                        .build());

        assertThat(getNoteListFragment().getNotebookId()).isEqualTo((Notebook.UNASSIGNED_NOTEBOOK_ID));

        NotePaneFragment fragment = (NotePaneFragment) getWorkspacePaneFragment();
        assertThat(fragment.getNoteId()).isEqualTo(215L);

        assertThat(subject.workspaceEmptyView).isGone();
    }

    private void assertNotebookNotesLoadedIntoNoteWorkspace() {
        verify(saveManager).save(anyLong(), any(Editable.class), any(Editable.class), saveCaptor.capture());
        saveCaptor.getValue().onSaveComplete();

        verify(noteRepository).asyncGetLastSavedNote(eq(2L), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(
                NoteBuilder.noteBuilder()
                        .id(215L)
                        .title("")
                        .body("")
                        .notebookId(9L)
                        .build());

        assertThat(getNoteListFragment().getNotebookId()).isEqualTo((2L));

        NotePaneFragment fragment = (NotePaneFragment) getWorkspacePaneFragment();
        assertThat(fragment.getNoteId()).isEqualTo(215L);
        assertThat(subject.workspaceEmptyView).isGone();
    }

    private void setupGetLastSavedNote_forOnCreate() {
        subject = Robolectric.setupActivity(MainActivity.class);

        verify(noteRepository).asyncGetLastSavedNote(eq(Notebook.UNASSIGNED_NOTEBOOK_ID), queryCaptor.capture());
        queryCaptor.getValue().onQueryNoteComplete(testNote);
        reset(noteRepository);
    }

    private Fragment getWorkspacePaneFragment() {
        return subject.getSupportFragmentManager().findFragmentById(R.id.workspace_fragment_container);
    }

    private NoteListFragment getNoteListFragment() {
        return (NoteListFragment) subject.getSupportFragmentManager().findFragmentById(R.id.note_list_fragment);
    }

    private Intent makeSearchResultIntent() {
        Intent intent = new Intent();
        intent.putExtra(RESULT_NOTE_ID, 33L);
        intent.putExtra(RESULT_NOTEBOOK_ID, 103L);
        intent.putExtra(RESULT_QUERY_ARGUMENTS, "previous search test query");
        return intent;
    }

}