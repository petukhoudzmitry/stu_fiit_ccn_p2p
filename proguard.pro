-injars      build/libs/PKS_P2P.jar
-outjars     build/libs/PKS_P2P_proguard.jar
-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-printseeds

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}

-dontwarn org.jetbrains.annotations.*