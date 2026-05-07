package com.gidexplayyt.mcauth.core;

import java.io.IOException;
import java.util.List;

public interface AuthStorage {
    List<AccountData> loadAccounts() throws IOException;
    void saveAccounts(List<AccountData> accounts) throws IOException;
}
