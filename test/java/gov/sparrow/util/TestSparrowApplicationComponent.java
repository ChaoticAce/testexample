package gov.sparrow.util;

import javax.inject.Singleton;

import dagger.Component;
import gov.sparrow.activity.MainActivityTest;
import gov.sparrow.activity.SearchActivityTest;
import gov.sparrow.adapter.NoteListAdapterTest;
import gov.sparrow.adapter.SearchResultsListAdapterTest;
import gov.sparrow.fragment.ActionDueDateFragmentTest;
import gov.sparrow.fragment.ActionEditFragmentTest;
import gov.sparrow.fragment.ActionItemSettingsDialogTest;
import gov.sparrow.fragment.ActionPaneFragmentTest;
import gov.sparrow.fragment.CreateNotebookDialogFragmentTest;
import gov.sparrow.fragment.DeleteNoteDialogFragmentTest;
import gov.sparrow.fragment.EditNotebookDialogFragmentTest;
import gov.sparrow.fragment.NotePaneFragmentTest;
import gov.sparrow.fragment.NotebookListFragmentTest;
import gov.sparrow.fragment.RestoreDialogFragmentTest;
import gov.sparrow.listeners.ActionItemSettingsDialogSaveListenerTest;
import gov.sparrow.provider.SparrowProviderTest;

@Singleton
@Component(modules = TestSparrowApplicationModule.class)
public interface TestSparrowApplicationComponent extends SparrowApplicationComponent {

    void inject(SparrowProviderTest sparrowProviderTest);

    void inject(NotePaneFragmentTest notePaneFragmentTest);

    void inject(CreateNotebookDialogFragmentTest createNotebookDialogFragmentTest);

    void inject(EditNotebookDialogFragmentTest editNotebookDialogFragmentTest);

    void inject(MainActivityTest mainActivityTest);

    void inject(SearchActivityTest searchActivityTest);

    void inject(ActionPaneFragmentTest actionPaneFragmentTest);

    void inject(NoteListAdapterTest noteListAdapterTest);

    void inject(NotebookListFragmentTest notebookListFragmentTest);

    void inject(SearchResultsListAdapterTest searchResultsListAdapterTest);

    void inject(ActionEditFragmentTest actionEditFragmentTest);

    void inject(ActionDueDateFragmentTest actionDueDateFragmentTest);

    void inject(DeleteNoteDialogFragmentTest deleteNoteDialogFragmentTest);

    void inject(RestoreDialogFragmentTest restoreDialogFragmentTest);

    void inject(ActionItemSettingsDialogTest actionItemSettingsDialogTest);

    void inject(ActionItemSettingsDialogSaveListenerTest actionItemSettingsDialogSaveListenerTest);

}