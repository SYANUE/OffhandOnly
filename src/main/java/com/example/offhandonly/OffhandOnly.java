package com.example.offhandonly;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 公共入口（双端）。
 *
 * <p>挖掘进度与能否收获都是<b>服务端权威</b>的，所以本模组必须在服务端也加载，
 * 配置也需要在服务端读取 —— 这就是这个入口存在的理由。
 */
public class OffhandOnly implements ModInitializer {

    public static final String MOD_ID = "offhandonly";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        OffhandOnlyConfig.load();
    }
}
