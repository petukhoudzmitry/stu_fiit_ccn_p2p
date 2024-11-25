-injars     build/libs/PKS_P2P.jar
-outjars     build/libs/PKS_P2P_proguard.jar
-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)

-keep public class com.pks.p2p.Main {
    public static void main(java.lang.String[]);
}

-dontskipnonpubliclibraryclasses