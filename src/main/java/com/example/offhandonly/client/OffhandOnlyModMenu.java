package com.example.offhandonly.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * ModMenu 入口：在“模组”列表里给 Offhand Only 一个配置按钮。
 *
 * <p>这个类只在 ModMenu 存在时才会被加载（入口点 {@code modmenu} 由 ModMenu 主动拉取），
 * 所以没装 ModMenu 的玩家不会触发任何 NoClassDefFoundError。
 */
public class OffhandOnlyModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return OffhandOnlyConfigScreen::new;
    }
}
