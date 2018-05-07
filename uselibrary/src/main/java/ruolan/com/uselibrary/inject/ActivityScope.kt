package ruolan.com.uselibrary.inject


import java.lang.annotation.Retention
import javax.inject.Scope
import java.lang.annotation.RetentionPolicy.RUNTIME

@Suppress("DEPRECATED_JAVA_ANNOTATION")
@Scope
@Retention(RUNTIME)
annotation class ActivityScope