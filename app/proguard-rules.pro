# RayNeo SDK: BaseMirrorActivity finds each screen's ViewBinding by reflection
# (generic superclass → Binding.inflate(LayoutInflater)), so keep bindings and SDK classes.
-keep class com.ffalcon.mercury.android.sdk.** { *; }
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
}
-keepattributes Signature
-keep class dev.sweitz.veniceglasses.ui.** extends com.ffalcon.mercury.android.sdk.ui.activity.BaseMirrorActivity { *; }

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
