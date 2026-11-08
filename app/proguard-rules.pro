# kotlinx.serialization keeps generated serializers via its own consumer rules.
-keepattributes *Annotation*, InnerClasses

# kotlinx.serialization: keep generated serializers of our @Serializable models
-keepclassmembers @kotlinx.serialization.Serializable class io.github.omeg4dev.greatuntis.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.omeg4dev.greatuntis.**$$serializer { *; }
