package gov.sparrow.util;

import android.support.v4.app.Fragment;

import gov.sparrow.listeners.ActionDragListener;

public class TestFragment extends Fragment implements ActionDragListener.DragCompleteListener {

    public int ACTION_DRAG_EVENT_OLD_POSITION;
    public int ACTION_DRAG_EVENT_NEW_POSITION;
    public boolean ACTION_DRAG_EVENT_COMPLETED = false;

    @Override
    public void actionDragEventCompleted(int newPosition, int oldPosition) {
        ACTION_DRAG_EVENT_COMPLETED = true;
        ACTION_DRAG_EVENT_NEW_POSITION = newPosition;
        ACTION_DRAG_EVENT_OLD_POSITION = oldPosition;
    }

}
