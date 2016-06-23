package gov.sparrow.util;

import android.app.Application;
import android.database.sqlite.SQLiteQueryBuilder;
import dagger.Module;
import dagger.Provides;
import gov.sparrow.database.SparrowDatabaseHelper;
import gov.sparrow.datasync.SparrowRestoreManager;
import gov.sparrow.managers.SaveManager;
import gov.sparrow.provider.SQLiteQueryBuilderFactory;
import gov.sparrow.provider.services.*;
import gov.sparrow.repository.ActionRepository;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.repository.NotebookRepository;

import javax.inject.Singleton;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Module
public class TestSparrowApplicationModule {

    private Application application;

    public TestSparrowApplicationModule(Application app) {
        application = app;
    }

    @Singleton
    @Provides
    SparrowDatabaseHelper sparrowDatabaseHelper() {
        return mock(SparrowDatabaseHelper.class);
    }

    @Singleton
    @Provides
    SQLiteQueryBuilder sqLiteQueryBuilder() {
        return mock(SQLiteQueryBuilder.class);
    }

    @Singleton
    @Provides
    SQLiteQueryBuilderFactory sqLiteQueryBuilderFactory() {
        return mock(SQLiteQueryBuilderFactory.class);
    }

    @Singleton
    @Provides
    TimeUtil timeUtil() {
        return mock(TimeUtil.class);
    }

    @Singleton
    @Provides
    NoteRepository noteRepository() {
        return mock(NoteRepository.class);
    }

    @Singleton
    @Provides
    ActionRepository actionRepository() {
        return mock(ActionRepository.class);
    }

    @Singleton
    @Provides
    NotebookRepository notebookRepository() {
        return mock(NotebookRepository.class);
    }

    @Singleton
    @Provides
    SaveManager saveManager() {
        SaveManager saveManager = mock(SaveManager.class);
        when(saveManager.isEnabled()).thenReturn(true);
        return saveManager;
    }

    @Singleton
    @Provides
    SparrowRestoreManager sparrowRestoreManager() {
        return mock(SparrowRestoreManager.class);
    }

    @Singleton
    @Provides
    NotebookService notebookService() {
        return mock(NotebookService.class);
    }

    @Singleton
    @Provides
    NoteService noteService() {
        return mock(NoteService.class);
    }

    @Singleton
    @Provides
    ActionService actionService() {
        return mock(ActionService.class);
    }

    @Singleton
    @Provides
    StyleService styleService() {
        return mock(StyleService.class);
    }

    @Singleton
    @Provides
    SearchService searchService() {
        return mock(SearchService.class);
    }

    @Singleton
    @Provides
    BackupService backupService() {
        return mock(BackupService.class);
    }

}
