package com.example.imageorganizer;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.rxjava3.RxDataStore;

public class DataStoreSingleton {
    RxDataStore<Preferences> dataStore;
    private static final DataStoreSingleton dataStoreInstance = new DataStoreSingleton();

    public static DataStoreSingleton getInstance() {
        return dataStoreInstance;
    }
    private DataStoreSingleton() {}
    public void setDataStore(RxDataStore<Preferences> dataStore) {
        this.dataStore = dataStore;
    }
    public RxDataStore<Preferences> getDataStore() {
        return dataStore;
    }

}
