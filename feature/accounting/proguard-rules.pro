# 默认 ProGuard 规则，骨架阶段保持空配置
# 项目演进中如需混淆，再在此补充具体 keep 规则

# Retrofit 相关 keep 规则（预留）
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson 序列化对象保留（预留）
-keep class com.accounting.app.data.model.** { *; }
