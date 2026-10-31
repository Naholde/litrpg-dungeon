package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.LitRPG;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/** Читает {@code data/litrpg/balance/*.json} при старте мира и на {@code /reload}. */
public final class BalanceLoader extends SimpleJsonResourceReloadListener {
    public BalanceLoader() {
        super(new Gson(), "balance");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, JsonElement> ours = new HashMap<>();
        files.forEach((id, json) -> {
            if (id.getNamespace().equals(LitRPG.MOD_ID)) ours.put(id.getPath(), json);
        });
        var errors = Balance.apply(ours);
        LitRPG.LOG.info("[LitRPG] Баланс загружен: {} разделов, ошибок: {}", Balance.keys().size(), errors.size());
    }
}
