# Add project specific ProGuard rules here.

# --- Firebase Firestore model classes ---
# Firestore (de)serializes these classes by field/getter name via reflection.
# With R8 minification on, renaming their members produces obfuscated document
# keys (a, b, c, ...), which corrupts every write and breaks reads. Keep the
# model classes and all their members unobfuscated.
-keep class com.tally.app.data.Circle { *; }
-keep class com.tally.app.domain.model.User { *; }

# Preserve @PropertyName / @Exclude annotations that map Kotlin properties to
# Firestore document fields (User relies on @PropertyName).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,*Annotation*
