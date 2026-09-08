# Release keep rules: minimal set for this zero-reflection app.
# Custom view inflated from XML by class name (LayoutInflater reflection).
-keepclassmembers class com.ullas.circlesearch.SelectionView {
    public <init>(...);
}
# Readable stack traces in release crashes (personal diagnostics).
-keepattributes SourceFile,LineNumberTable
