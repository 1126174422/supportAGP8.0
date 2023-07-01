package com.cs.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.cs.plugin.methodtime.MethodTimeTransform
import com.cs.plugin.timecost.TimeCostTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class TestPlugin  : Plugin<Project> {
    override fun apply(project: Project) {
        //这里appExtension获取方式与原transform api不同，可自行对比
        val appExtension = project.extensions.getByType(
            AndroidComponentsExtension::class.java
        )
        //读取配置文件
        project.extensions.create("TestPlugin", ConfigExtension::class.java)
        project.extensions.create("TestPluginNew", ConfigExtensionNew::class.java)
        //这里通过transformClassesWith替换了原registerTransform来注册字节码转换操作
        appExtension.onVariants { variant ->
            //传统方式，配置获取后直接使用单例存储，使用时读取
            PluginHelper.extension = project.extensions.getByType(
                ConfigExtension::class.java
            )
            val extensionNew = project.extensions.getByType(
                ConfigExtensionNew::class.java
            )
            //可以通过variant来获取当前编译环境的一些信息，最重要的是可以 variant.name 来区分是debug模式还是release模式编译
            variant.instrumentation.transformClassesWith(MethodTimeTransform::class.java, InstrumentationScope.ALL) {
            }
            variant.instrumentation.transformClassesWith(TimeCostTransform::class.java, InstrumentationScope.ALL) {
                //配置通过指定配置的类，携带到TimeCostTransform中
                it.packageNames.set(extensionNew.includePackages.toList())
                it.methodNames.set(extensionNew.includeMethods.toList())
                it.logTag.set(extensionNew.logTag)
            }
            //InstrumentationScope.ALL 配合 FramesComputationMode.COPY_FRAMES可以指定该字节码转换器在全局生效，包括第三方lib
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }
    }
}