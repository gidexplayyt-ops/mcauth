package com.gidexplayyt.mcauth.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonAuthStorage implements AuthStorage {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonAuthStorage(Path dataFolder) {
        this.file = dataFolder.resolve("accounts.json");
    }

    @Override
    public synchronized List<AccountData> loadAccounts() throws IOException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        return mapper.readValue(Files.newBufferedReader(file), new TypeReference<List<AccountData>>() {});
    }

    @Override
    public synchronized void saveAccounts(List<AccountData> accounts) throws IOException {
        if (!Files.exists(file.getParent())) {
            Files.createDirectories(file.getParent());
        }
        mapper.writerWithDefaultPrettyPrinter().writeValue(Files.newBufferedWriter(file), accounts);
    }
}
