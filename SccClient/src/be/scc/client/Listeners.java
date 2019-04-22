package be.scc.client;

import java.util.ArrayList;
import java.util.List;

interface SccListener {
    /**
     * Should be idempotent.
     * This function translates the model information to GUI and should not change any external state.
     */
    void sccModelChanged() throws Exception;
}

class SccDispatcher {
    private List<SccListener> listeners = new ArrayList<SccListener>();

    public void addListener(SccListener toAdd) {
        try {
            toAdd.sccModelChanged(); // Could be unhandy in some cases
        } catch (Exception e) {
            e.printStackTrace();
        }
        listeners.add(toAdd);
    }

    public void sccDispatchModelChanged() {
        for (SccListener hl : listeners) {
            try {
                hl.sccModelChanged();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
